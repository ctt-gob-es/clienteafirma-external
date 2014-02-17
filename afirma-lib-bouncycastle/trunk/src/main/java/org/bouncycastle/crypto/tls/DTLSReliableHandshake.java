package org.bouncycastle.crypto.tls;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.bouncycastle.util.Integers;

class DTLSReliableHandshake
{

    private final static int MAX_RECEIVE_AHEAD = 10;

    private final DTLSRecordLayer recordLayer;

    private TlsHandshakeHash hash = new DeferredHash();

    private Hashtable<Integer, DTLSReassembler> currentInboundFlight = new Hashtable<Integer, DTLSReassembler>();
    private Hashtable<Integer, DTLSReassembler> previousInboundFlight = null;
    private final Vector<Message> outboundFlight = new Vector<Message>();
    private boolean sending = true;

    private int message_seq = 0, next_receive_seq = 0;

    DTLSReliableHandshake(final TlsContext context, final DTLSRecordLayer transport)
    {
        this.recordLayer = transport;
        this.hash.init(context);
    }

    void notifyHelloComplete()
    {
        this.hash = this.hash.commit();
    }

    byte[] getCurrentHash()
    {
        final TlsHandshakeHash copyOfHash = this.hash.fork();
        final byte[] result = new byte[copyOfHash.getDigestSize()];
        copyOfHash.doFinal(result, 0);
        return result;
    }

    void sendMessage(final short msg_type, final byte[] body)
        throws IOException
    {

        if (!this.sending)
        {
            checkInboundFlight();
            this.sending = true;
            this.outboundFlight.removeAllElements();
        }

        final Message message = new Message(this.message_seq++, msg_type, body);

        this.outboundFlight.addElement(message);

        writeMessage(message);
        updateHandshakeMessagesDigest(message);
    }

    Message receiveMessage()
        throws IOException
    {

        if (this.sending)
        {
            this.sending = false;
            prepareInboundFlight();
        }

        // Check if we already have the next message waiting
        {
            final DTLSReassembler next = this.currentInboundFlight.get(Integers.valueOf(this.next_receive_seq));
            if (next != null)
            {
                final byte[] body = next.getBodyIfComplete();
                if (body != null)
                {
                    this.previousInboundFlight = null;
                    return updateHandshakeMessagesDigest(new Message(this.next_receive_seq++, next.getType(), body));
                }
            }
        }

        byte[] buf = null;

        // TODO Check the conditions under which we should reset this
        int readTimeoutMillis = 1000;

        for (; ; )
        {

            final int receiveLimit = this.recordLayer.getReceiveLimit();
            if (buf == null || buf.length < receiveLimit)
            {
                buf = new byte[receiveLimit];
            }

            // TODO Handle records containing multiple handshake messages

            try
            {
                for (; ; )
                {
                    final int received = this.recordLayer.receive(buf, 0, receiveLimit, readTimeoutMillis);
                    if (received < 0)
                    {
                        break;
                    }
                    if (received < 12)
                    {
                        continue;
                    }
                    final int fragment_length = TlsUtils.readUint24(buf, 9);
                    if (received != fragment_length + 12)
                    {
                        continue;
                    }
                    final int seq = TlsUtils.readUint16(buf, 4);
                    if (seq > this.next_receive_seq + MAX_RECEIVE_AHEAD)
                    {
                        continue;
                    }
                    final short msg_type = TlsUtils.readUint8(buf, 0);
                    final int length = TlsUtils.readUint24(buf, 1);
                    final int fragment_offset = TlsUtils.readUint24(buf, 6);
                    if (fragment_offset + fragment_length > length)
                    {
                        continue;
                    }

                    if (seq < this.next_receive_seq)
                    {
                        /*
                         * NOTE: If we receive the previous flight of incoming messages in full
                         * again, retransmit our last flight
                         */
                        if (this.previousInboundFlight != null)
                        {
                            final DTLSReassembler reassembler = this.previousInboundFlight.get(Integers
                                .valueOf(seq));
                            if (reassembler != null)
                            {

                                reassembler.contributeFragment(msg_type, length, buf, 12, fragment_offset,
                                    fragment_length);

                                if (checkAll(this.previousInboundFlight))
                                {

                                    resendOutboundFlight();

                                    /*
                                     * TODO[DTLS] implementations SHOULD back off handshake packet
                                     * size during the retransmit backoff.
                                     */
                                    readTimeoutMillis = Math.min(readTimeoutMillis * 2, 60000);

                                    resetAll(this.previousInboundFlight);
                                }
                            }
                        }
                    }
                    else
                    {

                        DTLSReassembler reassembler = this.currentInboundFlight.get(Integers.valueOf(seq));
                        if (reassembler == null)
                        {
                            reassembler = new DTLSReassembler(msg_type, length);
                            this.currentInboundFlight.put(Integers.valueOf(seq), reassembler);
                        }

                        reassembler.contributeFragment(msg_type, length, buf, 12, fragment_offset, fragment_length);

                        if (seq == this.next_receive_seq)
                        {
                            final byte[] body = reassembler.getBodyIfComplete();
                            if (body != null)
                            {
                                this.previousInboundFlight = null;
                                return updateHandshakeMessagesDigest(new Message(this.next_receive_seq++,
                                    reassembler.getType(), body));
                            }
                        }
                    }
                }
            }
            catch (final IOException e)
            {
                // NOTE: Assume this is a timeout for the moment
            }

            resendOutboundFlight();

            /*
             * TODO[DTLS] implementations SHOULD back off handshake packet size during the
             * retransmit backoff.
             */
            readTimeoutMillis = Math.min(readTimeoutMillis * 2, 60000);
        }
    }

    void finish()
    {
        DTLSHandshakeRetransmit retransmit = null;
        if (!this.sending)
        {
            checkInboundFlight();
        }
        else if (this.currentInboundFlight != null)
        {
            /*
             * RFC 6347 4.2.4. In addition, for at least twice the default MSL defined for [TCP],
             * when in the FINISHED state, the node that transmits the last flight (the server in an
             * ordinary handshake or the client in a resumed handshake) MUST respond to a retransmit
             * of the peer's last flight with a retransmit of the last flight.
             */
            retransmit = new DTLSHandshakeRetransmit()
            {
                @Override
				public void receivedHandshakeRecord(final int epoch, final byte[] buf, final int off, final int len)
                    throws IOException
                {
                    /*
                     * TODO Need to handle the case where the previous inbound flight contains
                     * messages from two epochs.
                     */
                    if (len < 12)
                    {
                        return;
                    }
                    final int fragment_length = TlsUtils.readUint24(buf, off + 9);
                    if (len != fragment_length + 12)
                    {
                        return;
                    }
                    final int seq = TlsUtils.readUint16(buf, off + 4);
                    if (seq >= DTLSReliableHandshake.this.next_receive_seq)
                    {
                        return;
                    }

                    final short msg_type = TlsUtils.readUint8(buf, off);

                    // TODO This is a hack that only works until we try to support renegotiation
                    final int expectedEpoch = msg_type == HandshakeType.finished ? 1 : 0;
                    if (epoch != expectedEpoch)
                    {
                        return;
                    }

                    final int length = TlsUtils.readUint24(buf, off + 1);
                    final int fragment_offset = TlsUtils.readUint24(buf, off + 6);
                    if (fragment_offset + fragment_length > length)
                    {
                        return;
                    }

                    final DTLSReassembler reassembler = DTLSReliableHandshake.this.currentInboundFlight.get(Integers.valueOf(seq));
                    if (reassembler != null)
                    {
                        reassembler.contributeFragment(msg_type, length, buf, off + 12, fragment_offset,
                            fragment_length);
                        if (checkAll(DTLSReliableHandshake.this.currentInboundFlight))
                        {
                            resendOutboundFlight();
                            resetAll(DTLSReliableHandshake.this.currentInboundFlight);
                        }
                    }
                }
            };
        }

        this.recordLayer.handshakeSuccessful(retransmit);
    }

    void resetHandshakeMessagesDigest()
    {
        this.hash.reset();
    }

    /**
     * Check that there are no "extra" messages left in the current inbound flight
     */
    private void checkInboundFlight()
    {
        final Enumeration<Integer> e = this.currentInboundFlight.keys();
        while (e.hasMoreElements())
        {
            final Integer key = e.nextElement();
            if (key.intValue() >= this.next_receive_seq)
            {
                // TODO Should this be considered an error?
            }
        }
    }

    private void prepareInboundFlight()
    {
        resetAll(this.currentInboundFlight);
        this.previousInboundFlight = this.currentInboundFlight;
        this.currentInboundFlight = new Hashtable<Integer, DTLSReassembler>();
    }

    private void resendOutboundFlight()
        throws IOException
    {
        this.recordLayer.resetWriteEpoch();
        for (int i = 0; i < this.outboundFlight.size(); ++i)
        {
            writeMessage(this.outboundFlight.elementAt(i));
        }
    }

    private Message updateHandshakeMessagesDigest(final Message message)
        throws IOException
    {
        if (message.getType() != HandshakeType.hello_request)
        {
            final byte[] body = message.getBody();
            final byte[] buf = new byte[12];
            TlsUtils.writeUint8(message.getType(), buf, 0);
            TlsUtils.writeUint24(body.length, buf, 1);
            TlsUtils.writeUint16(message.getSeq(), buf, 4);
            TlsUtils.writeUint24(0, buf, 6);
            TlsUtils.writeUint24(body.length, buf, 9);
            this.hash.update(buf, 0, buf.length);
            this.hash.update(body, 0, body.length);
        }
        return message;
    }

    private void writeMessage(final Message message)
        throws IOException
    {

        final int sendLimit = this.recordLayer.getSendLimit();
        final int fragmentLimit = sendLimit - 12;

        // TODO Support a higher minimum fragment size?
        if (fragmentLimit < 1)
        {
            // TODO Should we be throwing an exception here?
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }

        final int length = message.getBody().length;

        // NOTE: Must still send a fragment if body is empty
        int fragment_offset = 0;
        do
        {
            final int fragment_length = Math.min(length - fragment_offset, fragmentLimit);
            writeHandshakeFragment(message, fragment_offset, fragment_length);
            fragment_offset += fragment_length;
        }
        while (fragment_offset < length);
    }

    private void writeHandshakeFragment(final Message message, final int fragment_offset, final int fragment_length)
        throws IOException
    {

        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        TlsUtils.writeUint8(message.getType(), buf);
        TlsUtils.writeUint24(message.getBody().length, buf);
        TlsUtils.writeUint16(message.getSeq(), buf);
        TlsUtils.writeUint24(fragment_offset, buf);
        TlsUtils.writeUint24(fragment_length, buf);
        buf.write(message.getBody(), fragment_offset, fragment_length);

        final byte[] fragment = buf.toByteArray();

        this.recordLayer.send(fragment, 0, fragment.length);
    }

    private static boolean checkAll(final Hashtable<Integer, DTLSReassembler> inboundFlight)
    {
        final Enumeration<DTLSReassembler> e = inboundFlight.elements();
        while (e.hasMoreElements())
        {
            if (e.nextElement().getBodyIfComplete() == null)
            {
                return false;
            }
        }
        return true;
    }

    private static void resetAll(final Hashtable<Integer, DTLSReassembler> inboundFlight)
    {
        final Enumeration<DTLSReassembler> e = inboundFlight.elements();
        while (e.hasMoreElements())
        {
            e.nextElement().reset();
        }
    }

    static class Message
    {

        private final int message_seq;
        private final short msg_type;
        private final byte[] body;

        Message(final int message_seq, final short msg_type, final byte[] body)
        {
            this.message_seq = message_seq;
            this.msg_type = msg_type;
            this.body = body;
        }

        public int getSeq()
        {
            return this.message_seq;
        }

        public short getType()
        {
            return this.msg_type;
        }

        public byte[] getBody()
        {
            return this.body;
        }
    }
}
