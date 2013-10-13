package org.bouncycastle.jcajce.provider.symmetric;

import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseMac;
import org.bouncycastle.jcajce.provider.util.AlgorithmProvider;

public final class SipHash
{
    private SipHash()
    {
    }
    
    public static class Mac
        extends BaseMac
    {
        public Mac()
        {
            super(new org.bouncycastle.crypto.macs.SipHash());
        }
    }

    public static class Mac48
        extends BaseMac
    {
        public Mac48()
        {
            super(new org.bouncycastle.crypto.macs.SipHash(4, 8));
        }
    }

    public static class Mappings
        extends AlgorithmProvider
    {
        private static final String PREFIX = SipHash.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {
            provider.addAlgorithm("Mac.SIPHASH", PREFIX + "$Mac");
            provider.addAlgorithm("Alg.Alias.Mac.SIPHASH-2-4", "SIPHASH");
            provider.addAlgorithm("Mac.SIPHASH-4-8", PREFIX + "$Mac48");
        }
    }
}
