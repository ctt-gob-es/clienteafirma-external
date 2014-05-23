package org.bouncycastle.math.ec;

import java.math.BigInteger;
import java.util.Random;

import org.bouncycastle.util.BigIntegers;

/**
 * base class for an elliptic curve
 */
public abstract class ECCurve
{
    public static final int COORD_AFFINE = 0;
    public static final int COORD_HOMOGENEOUS = 1;
    public static final int COORD_JACOBIAN = 2;
    public static final int COORD_JACOBIAN_CHUDNOVSKY = 3;
    public static final int COORD_JACOBIAN_MODIFIED = 4;
    public static final int COORD_LAMBDA_AFFINE = 5;
    public static final int COORD_LAMBDA_PROJECTIVE = 6;
    public static final int COORD_SKEWED = 7;

    public static int[] getAllCoordinateSystems()
    {
        return new int[]{ COORD_AFFINE, COORD_HOMOGENEOUS, COORD_JACOBIAN, COORD_JACOBIAN_CHUDNOVSKY,
            COORD_JACOBIAN_MODIFIED, COORD_LAMBDA_AFFINE, COORD_LAMBDA_PROJECTIVE, COORD_SKEWED };
    }

    public class Config
    {
        protected int coord;
        protected ECMultiplier multiplier;

        Config(int coord, ECMultiplier multiplier)
        {
            this.coord = coord;
            this.multiplier = multiplier;
        }

        public Config setCoordinateSystem(int coord)
        {
            this.coord = coord;
            return this;
        }

        public Config setMultiplier(ECMultiplier multiplier)
        {
            this.multiplier = multiplier;
            return this;
        }

        public ECCurve create()
        {
            if (!supportsCoordinateSystem(coord))
            {
                throw new IllegalStateException("unsupported coordinate system");
            }

            ECCurve c = cloneCurve();
            if (c == ECCurve.this)
            {
                throw new IllegalStateException("implementation returned current curve");
            }

            c.coord = coord;
            c.multiplier = multiplier;

            return c;
        }
    }

    protected ECFieldElement a, b;
    protected int coord = COORD_AFFINE;
    protected ECMultiplier multiplier = null;

    public abstract int getFieldSize();

    public abstract ECFieldElement fromBigInteger(BigInteger x);

    public Config configure()
    {
        return new Config(this.coord, this.multiplier);
    }

    public ECPoint createPoint(BigInteger x, BigInteger y)
    {
        return createPoint(x, y, false);
    }

    /**
     * @deprecated per-point compression property will be removed, use {@link #createPoint(BigInteger, BigInteger)}
     * and refer {@link ECPoint#getEncoded(boolean)}
     */
    public ECPoint createPoint(BigInteger x, BigInteger y, boolean withCompression)
    {
        return createRawPoint(fromBigInteger(x), fromBigInteger(y), withCompression);
    }

    protected abstract ECCurve cloneCurve();

    protected abstract ECPoint createRawPoint(ECFieldElement x, ECFieldElement y, boolean withCompression);

    protected ECMultiplier createDefaultMultiplier()
    {
        return new WNafL2RMultiplier();
    }

    public boolean supportsCoordinateSystem(int coord)
    {
        return coord == COORD_AFFINE;
    }

    public PreCompInfo getPreCompInfo(ECPoint p)
    {
        checkPoint(p);
        return p.preCompInfo;
    }

    /**
     * Sets the <code>PreCompInfo</code> for a point on this curve. Used by
     * <code>ECMultiplier</code>s to save the precomputation for this <code>ECPoint</code> for use
     * by subsequent multiplication.
     * 
     * @param point
     *            The <code>ECPoint</code> to store precomputations for.
     * @param preCompInfo
     *            The values precomputed by the <code>ECMultiplier</code>.
     */
    public void setPreCompInfo(ECPoint point, PreCompInfo preCompInfo)
    {
        checkPoint(point);
        point.preCompInfo = preCompInfo;
    }

    public ECPoint importPoint(ECPoint p)
    {
        if (this == p.getCurve())
        {
            return p;
        }
        if (p.isInfinity())
        {
            return getInfinity();
        }

        // TODO Default behaviour could be improved if the two curves have the same coordinate system by copying any Z coordinates.
        p = p.normalize();

        return createPoint(p.getXCoord().toBigInteger(), p.getYCoord().toBigInteger(), p.withCompression);
    }

    /**
     * Normalization ensures that any projective coordinate is 1, and therefore that the x, y
     * coordinates reflect those of the equivalent point in an affine coordinate system. Where more
     * than one point is to be normalized, this method will generally be more efficient than
     * normalizing each point separately.
     * 
     * @param points
     *            An array of points that will be updated in place with their normalized versions,
     *            where necessary
     */
    public void normalizeAll(ECPoint[] points)
    {
        checkPoints(points);

        if (this.getCoordinateSystem() == ECCurve.COORD_AFFINE)
        {
            return;
        }

        /*
         * Figure out which of the points actually need to be normalized
         */
        ECFieldElement[] zs = new ECFieldElement[points.length];
        int[] indices = new int[points.length];
        int count = 0;
        for (int i = 0; i < points.length; ++i)
        {
            ECPoint p = points[i];
            if (null != p && !p.isNormalized())
            {
                zs[count] = p.getZCoord(0);
                indices[count++] = i;
            }
        }

        if (count == 0)
        {
            return;
        }

        ECAlgorithms.implMontgomeryTrick(zs, 0, count);

        for (int j = 0; j < count; ++j)
        {
            int index = indices[j];
            points[index] = points[index].normalize(zs[j]);
        }
    }

    public abstract ECPoint getInfinity();

    public ECFieldElement getA()
    {
        return a;
    }

    public ECFieldElement getB()
    {
        return b;
    }

    public int getCoordinateSystem()
    {
        return coord;
    }

    protected abstract ECPoint decompressPoint(int yTilde, BigInteger X1);

    /**
     * Sets the default <code>ECMultiplier</code>, unless already set. 
     */
    public ECMultiplier getMultiplier()
    {
        if (this.multiplier == null)
        {
            this.multiplier = createDefaultMultiplier();
        }
        return this.multiplier;
    }

    /**
     * Decode a point on this curve from its ASN.1 encoding. The different
     * encodings are taken account of, including point compression for
     * <code>F<sub>p</sub></code> (X9.62 s 4.2.1 pg 17).
     * @return The decoded point.
     */
    public ECPoint decodePoint(byte[] encoded)
    {
        ECPoint p = null;
        int expectedLength = (getFieldSize() + 7) / 8;

        switch (encoded[0])
        {
        case 0x00: // infinity
        {
            if (encoded.length != 1)
            {
                throw new IllegalArgumentException("Incorrect length for infinity encoding");
            }

            p = getInfinity();
            break;
        }
        case 0x02: // compressed
        case 0x03: // compressed
        {
            if (encoded.length != (expectedLength + 1))
            {
                throw new IllegalArgumentException("Incorrect length for compressed encoding");
            }

            int yTilde = encoded[0] & 1;
            BigInteger X = BigIntegers.fromUnsignedByteArray(encoded, 1, expectedLength);

            p = decompressPoint(yTilde, X);
            break;
        }
        case 0x04: // uncompressed
        case 0x06: // hybrid
        case 0x07: // hybrid
        {
            if (encoded.length != (2 * expectedLength + 1))
            {
                throw new IllegalArgumentException("Incorrect length for uncompressed/hybrid encoding");
            }

            BigInteger X = BigIntegers.fromUnsignedByteArray(encoded, 1, expectedLength);
            BigInteger Y = BigIntegers.fromUnsignedByteArray(encoded, 1 + expectedLength, expectedLength);

            p = createPoint(X, Y);
            break;
        }
        default:
            throw new IllegalArgumentException("Invalid point encoding 0x" + Integer.toString(encoded[0], 16));
        }

        return p;
    }

    protected void checkPoint(ECPoint point)
    {
        if (null == point || (this != point.getCurve()))
        {
            throw new IllegalArgumentException("'point' must be non-null and on this curve");
        }
    }

    protected void checkPoints(ECPoint[] points)
    {
        if (points == null)
        {
            throw new IllegalArgumentException("'points' cannot be null");
        }

        for (int i = 0; i < points.length; ++i)
        {
            ECPoint point = points[i];
            if (null != point && this != point.getCurve())
            {
                throw new IllegalArgumentException("'points' entries must be null or on this curve");
            }
        }
    }

    /**
     * Elliptic curve over Fp
     */
    public static class Fp extends ECCurve
    {
        private static final int FP_DEFAULT_COORDS = COORD_JACOBIAN_MODIFIED;

        BigInteger q, r;
        ECPoint.Fp infinity;

        public Fp(BigInteger q, BigInteger a, BigInteger b)
        {
            this.q = q;
            this.r = ECFieldElement.Fp.calculateResidue(q);
            this.infinity = new ECPoint.Fp(this, null, null);

            this.a = fromBigInteger(a);
            this.b = fromBigInteger(b);
            this.coord = FP_DEFAULT_COORDS;
        }

        protected Fp(BigInteger q, BigInteger r, ECFieldElement a, ECFieldElement b)
        {
            this.q = q;
            this.r = r;
            this.infinity = new ECPoint.Fp(this, null, null);

            this.a = a;
            this.b = b;
            this.coord = FP_DEFAULT_COORDS;
        }

        protected ECCurve cloneCurve()
        {
            return new Fp(q, r, a, b);
        }

        public boolean supportsCoordinateSystem(int coord)
        {
            switch (coord)
            {
            case COORD_AFFINE:
            case COORD_HOMOGENEOUS:
            case COORD_JACOBIAN:
            case COORD_JACOBIAN_MODIFIED:
                return true;
            default:
                return false;
            }
        }

        public BigInteger getQ()
        {
            return q;
        }

        public int getFieldSize()
        {
            return q.bitLength();
        }

        public ECFieldElement fromBigInteger(BigInteger x)
        {
            return new ECFieldElement.Fp(this.q, this.r, x);
        }

        protected ECPoint createRawPoint(ECFieldElement x, ECFieldElement y, boolean withCompression)
        {
            return new ECPoint.Fp(this, x, y, withCompression);
        }

        public ECPoint importPoint(ECPoint p)
        {
            if (this != p.getCurve() && this.getCoordinateSystem() == COORD_JACOBIAN && !p.isInfinity())
            {
                switch (p.getCurve().getCoordinateSystem())
                {
                case COORD_JACOBIAN:
                case COORD_JACOBIAN_CHUDNOVSKY:
                case COORD_JACOBIAN_MODIFIED:
                    return new ECPoint.Fp(this,
                        fromBigInteger(p.x.toBigInteger()),
                        fromBigInteger(p.y.toBigInteger()),
                        new ECFieldElement[]{ fromBigInteger(p.zs[0].toBigInteger()) },
                        p.withCompression);
                default:
                    break;
                }
            }

            return super.importPoint(p);
        }

        protected ECPoint decompressPoint(int yTilde, BigInteger X1)
        {
            ECFieldElement x = fromBigInteger(X1);
            ECFieldElement alpha = x.multiply(x.square().add(a)).add(b);
            ECFieldElement beta = alpha.sqrt();

            //
            // if we can't find a sqrt we haven't got a point on the
            // curve - run!
            //
            if (beta == null)
            {
                throw new RuntimeException("Invalid point compression");
            }

            BigInteger betaValue = beta.toBigInteger();
            if (betaValue.testBit(0) != (yTilde == 1))
            {
                // Use the other root
                beta = fromBigInteger(q.subtract(betaValue));
            }

            return new ECPoint.Fp(this, x, beta, true);
        }

        public ECPoint getInfinity()
        {
            return infinity;
        }

        public boolean equals(
            Object anObject) 
        {
            if (anObject == this) 
            {
                return true;
            }

            if (!(anObject instanceof ECCurve.Fp)) 
            {
                return false;
            }

            ECCurve.Fp other = (ECCurve.Fp) anObject;

            return this.q.equals(other.q) 
                    && a.equals(other.a) && b.equals(other.b);
        }

        public int hashCode() 
        {
            return a.hashCode() ^ b.hashCode() ^ q.hashCode();
        }
    }

    /**
     * Elliptic curves over F2m. The Weierstrass equation is given by
     * <code>y<sup>2</sup> + xy = x<sup>3</sup> + ax<sup>2</sup> + b</code>.
     */
    public static class F2m extends ECCurve
    {
        private static final int F2M_DEFAULT_COORDS = COORD_AFFINE;

        /**
         * The exponent <code>m</code> of <code>F<sub>2<sup>m</sup></sub></code>.
         */
        private int m;  // can't be final - JDK 1.1

        /**
         * TPB: The integer <code>k</code> where <code>x<sup>m</sup> +
         * x<sup>k</sup> + 1</code> represents the reduction polynomial
         * <code>f(z)</code>.<br>
         * PPB: The integer <code>k1</code> where <code>x<sup>m</sup> +
         * x<sup>k3</sup> + x<sup>k2</sup> + x<sup>k1</sup> + 1</code>
         * represents the reduction polynomial <code>f(z)</code>.<br>
         */
        private int k1;  // can't be final - JDK 1.1

        /**
         * TPB: Always set to <code>0</code><br>
         * PPB: The integer <code>k2</code> where <code>x<sup>m</sup> +
         * x<sup>k3</sup> + x<sup>k2</sup> + x<sup>k1</sup> + 1</code>
         * represents the reduction polynomial <code>f(z)</code>.<br>
         */
        private int k2;  // can't be final - JDK 1.1

        /**
         * TPB: Always set to <code>0</code><br>
         * PPB: The integer <code>k3</code> where <code>x<sup>m</sup> +
         * x<sup>k3</sup> + x<sup>k2</sup> + x<sup>k1</sup> + 1</code>
         * represents the reduction polynomial <code>f(z)</code>.<br>
         */
        private int k3;  // can't be final - JDK 1.1

        /**
         * The order of the base point of the curve.
         */
        private BigInteger n;  // can't be final - JDK 1.1

        /**
         * The cofactor of the curve.
         */
        private BigInteger h;  // can't be final - JDK 1.1
        
         /**
         * The point at infinity on this curve.
         */
        private ECPoint.F2m infinity;  // can't be final - JDK 1.1

        /**
         * The parameter <code>&mu;</code> of the elliptic curve if this is
         * a Koblitz curve.
         */
        private byte mu = 0;

        /**
         * The auxiliary values <code>s<sub>0</sub></code> and
         * <code>s<sub>1</sub></code> used for partial modular reduction for
         * Koblitz curves.
         */
        private BigInteger[] si = null;

        /**
         * Constructor for Trinomial Polynomial Basis (TPB).
         * @param m  The exponent <code>m</code> of
         * <code>F<sub>2<sup>m</sup></sub></code>.
         * @param k The integer <code>k</code> where <code>x<sup>m</sup> +
         * x<sup>k</sup> + 1</code> represents the reduction
         * polynomial <code>f(z)</code>.
         * @param a The coefficient <code>a</code> in the Weierstrass equation
         * for non-supersingular elliptic curves over
         * <code>F<sub>2<sup>m</sup></sub></code>.
         * @param b The coefficient <code>b</code> in the Weierstrass equation
         * for non-supersingular elliptic curves over
         * <code>F<sub>2<sup>m</sup></sub></code>.
         */
        public F2m(
            int m,
            int k,
            BigInteger a,
            BigInteger b)
        {
            this(m, k, 0, 0, a, b, null, null);
        }

        /**
         * Constructor for Trinomial Polynomial Basis (TPB).
         * @param m  The exponent <code>m</code> of
         * <code>F<sub>2<sup>m</sup></sub></code>.
         * @param k The integer <code>k</code> where <code>x<sup>m</sup> +
         * x<sup>k</sup> + 1</code> represents the reduction
         * polynomial <code>f(z)</code>.
         * @param a The coefficient <code>a</code> in the Weierstrass equation
         * for non-supersingular elliptic curves over
         * <code>F<sub>2<sup>m</sup></sub></code>.
         * @param b The coefficient <code>b</code> in the Weierstrass equation
         * for non-supersingular elliptic curves over
         * <code>F<sub>2<sup>m</sup></sub></code>.
         * @param n The order of the main subgroup of the elliptic curve.
         * @param h The cofactor of the elliptic curve, i.e.
         * <code>#E<sub>a</sub>(F<sub>2<sup>m</sup></sub>) = h * n</code>.
         */
        public F2m(
            int m, 
            int k, 
            BigInteger a, 
            BigInteger b,
            BigInteger n,
            BigInteger h)
        {
            this(m, k, 0, 0, a, b, n, h);
        }

        /**
         * Constructor for Pentanomial Polynomial Basis (PPB).
         * @param m  The exponent <code>m</code> of
         * <code>F<sub>2<sup>m</sup></sub></code>.
         * @param k1 The integer <code>k1</code> where <code>x<sup>m</sup> +
         * x<sup>k3</sup> + x<sup>k2</sup> + x<sup>k1</sup> + 1</code>
         * represents the reduction polynomial <code>f(z)</code>.
         * @param k2 The integer <code>k2</code> where <code>x<sup>m</sup> +
         * x<sup>k3</sup> + x<sup>k2</sup> + x<sup>k1</sup> + 1</code>
         * represents the reduction polynomial <code>f(z)</code>.
         * @param k3 The integer <code>k3</code> where <code>x<sup>m</sup> +
         * x<sup>k3</sup> + x<sup>k2</sup> + x<sup>k1</sup> + 1</code>
         * represents the reduction polynomial <code>f(z)</code>.
         * @param a The coefficient <code>a</code> in the Weierstrass equation
         * for non-supersingular elliptic curves over
         * <code>F<sub>2<sup>m</sup></sub></code>.
         * @param b The coefficient <code>b</code> in the Weierstrass equation
         * for non-supersingular elliptic curves over
         * <code>F<sub>2<sup>m</sup></sub></code>.
         */
        public F2m(
            int m,
            int k1,
            int k2,
            int k3,
            BigInteger a,
            BigInteger b)
        {
            this(m, k1, k2, k3, a, b, null, null);
        }

        /**
         * Constructor for Pentanomial Polynomial Basis (PPB).
         * @param m  The exponent <code>m</code> of
         * <code>F<sub>2<sup>m</sup></sub></code>.
         * @param k1 The integer <code>k1</code> where <code>x<sup>m</sup> +
         * x<sup>k3</sup> + x<sup>k2</sup> + x<sup>k1</sup> + 1</code>
         * represents the reduction polynomial <code>f(z)</code>.
         * @param k2 The integer <code>k2</code> where <code>x<sup>m</sup> +
         * x<sup>k3</sup> + x<sup>k2</sup> + x<sup>k1</sup> + 1</code>
         * represents the reduction polynomial <code>f(z)</code>.
         * @param k3 The integer <code>k3</code> where <code>x<sup>m</sup> +
         * x<sup>k3</sup> + x<sup>k2</sup> + x<sup>k1</sup> + 1</code>
         * represents the reduction polynomial <code>f(z)</code>.
         * @param a The coefficient <code>a</code> in the Weierstrass equation
         * for non-supersingular elliptic curves over
         * <code>F<sub>2<sup>m</sup></sub></code>.
         * @param b The coefficient <code>b</code> in the Weierstrass equation
         * for non-supersingular elliptic curves over
         * <code>F<sub>2<sup>m</sup></sub></code>.
         * @param n The order of the main subgroup of the elliptic curve.
         * @param h The cofactor of the elliptic curve, i.e.
         * <code>#E<sub>a</sub>(F<sub>2<sup>m</sup></sub>) = h * n</code>.
         */
        public F2m(
            int m, 
            int k1, 
            int k2, 
            int k3,
            BigInteger a, 
            BigInteger b,
            BigInteger n,
            BigInteger h)
        {
            this.m = m;
            this.k1 = k1;
            this.k2 = k2;
            this.k3 = k3;
            this.n = n;
            this.h = h;

            if (k1 == 0)
            {
                throw new IllegalArgumentException("k1 must be > 0");
            }

            if (k2 == 0)
            {
                if (k3 != 0)
                {
                    throw new IllegalArgumentException("k3 must be 0 if k2 == 0");
                }
            }
            else
            {
                if (k2 <= k1)
                {
                    throw new IllegalArgumentException("k2 must be > k1");
                }

                if (k3 <= k2)
                {
                    throw new IllegalArgumentException("k3 must be > k2");
                }
            }

            this.infinity = new ECPoint.F2m(this, null, null);
            this.a = fromBigInteger(a);
            this.b = fromBigInteger(b);
            this.coord = F2M_DEFAULT_COORDS;
        }

        protected F2m(int m, int k1, int k2, int k3, ECFieldElement a, ECFieldElement b, BigInteger n, BigInteger h)
        {
            this.m = m;
            this.k1 = k1;
            this.k2 = k2;
            this.k3 = k3;
            this.n = n;
            this.h = h;

            this.infinity = new ECPoint.F2m(this, null, null);
            this.a = a;
            this.b = b;
            this.coord = F2M_DEFAULT_COORDS;
        }

        protected ECCurve cloneCurve()
        {
            return new F2m(m, k1, k2, k3, a, b, n, h);
        }

        public boolean supportsCoordinateSystem(int coord)
        {
            switch (coord)
            {
            case COORD_AFFINE:
            case COORD_HOMOGENEOUS:
            case COORD_LAMBDA_PROJECTIVE:
                return true;
            default:
                return false;
            }
        }

        protected ECMultiplier createDefaultMultiplier()
        {
            if (isKoblitz())
            {
                return new WTauNafMultiplier();
            }

            return super.createDefaultMultiplier();
        }

        public int getFieldSize()
        {
            return m;
        }

        public ECFieldElement fromBigInteger(BigInteger x)
        {
            return new ECFieldElement.F2m(this.m, this.k1, this.k2, this.k3, x);
        }

        public ECPoint createPoint(BigInteger x, BigInteger y, boolean withCompression)
        {
            ECFieldElement X = fromBigInteger(x), Y = fromBigInteger(y);

            switch (this.getCoordinateSystem())
            {
            case COORD_LAMBDA_AFFINE:
            case COORD_LAMBDA_PROJECTIVE:
            {
                if (!X.isZero())
                {
                    // Y becomes Lambda (X + Y/X) here
                    Y = Y.divide(X).add(X);
                }
                break;
            }
            default:
            {
                break;
            }
            }

            return createRawPoint(X, Y, withCompression);
        }

        protected ECPoint createRawPoint(ECFieldElement x, ECFieldElement y, boolean withCompression)
        {
            return new ECPoint.F2m(this, x, y, withCompression);
        }

        public ECPoint getInfinity()
        {
            return infinity;
        }

        /**
         * Returns true if this is a Koblitz curve (ABC curve).
         * @return true if this is a Koblitz curve (ABC curve), false otherwise
         */
        public boolean isKoblitz()
        {
            return n != null && h != null && a.bitLength() <= 1 && b.bitLength() == 1;
        }

        /**
         * Returns the parameter <code>&mu;</code> of the elliptic curve.
         * @return <code>&mu;</code> of the elliptic curve.
         * @throws IllegalArgumentException if the given ECCurve is not a
         * Koblitz curve.
         */
        synchronized byte getMu()
        {
            if (mu == 0)
            {
                mu = Tnaf.getMu(this);
            }
            return mu;
        }

        /**
         * @return the auxiliary values <code>s<sub>0</sub></code> and
         * <code>s<sub>1</sub></code> used for partial modular reduction for
         * Koblitz curves.
         */
        synchronized BigInteger[] getSi()
        {
            if (si == null)
            {
                si = Tnaf.getSi(this);
            }
            return si;
        }

        /**
         * Decompresses a compressed point P = (xp, yp) (X9.62 s 4.2.2).
         * 
         * @param yTilde
         *            ~yp, an indication bit for the decompression of yp.
         * @param X1
         *            The field element xp.
         * @return the decompressed point.
         */
        protected ECPoint decompressPoint(int yTilde, BigInteger X1)
        {
            ECFieldElement xp = fromBigInteger(X1);
            ECFieldElement yp = null;
            if (xp.isZero())
            {
                yp = (ECFieldElement.F2m)b;
                for (int i = 0; i < m - 1; i++)
                {
                    yp = yp.square();
                }
            }
            else
            {
                ECFieldElement beta = xp.add(a).add(b.multiply(xp.square().invert()));
                ECFieldElement z = solveQuadraticEquation(beta);
                if (z == null)
                {
                    throw new IllegalArgumentException("Invalid point compression");
                }
                if (z.testBitZero() != (yTilde == 1))
                {
                    z = z.addOne();
                }

                yp = xp.multiply(z);

                switch (this.getCoordinateSystem())
                {
                case COORD_LAMBDA_AFFINE:
                case COORD_LAMBDA_PROJECTIVE:
                {
                    yp = yp.divide(xp).add(xp);
                    break;
                }
                default:
                {
                    break;
                }
                }
            }

            return new ECPoint.F2m(this, xp, yp, true);
        }
        
        /**
         * Solves a quadratic equation <code>z<sup>2</sup> + z = beta</code>(X9.62
         * D.1.6) The other solution is <code>z + 1</code>.
         * 
         * @param beta
         *            The value to solve the quadratic equation for.
         * @return the solution for <code>z<sup>2</sup> + z = beta</code> or
         *         <code>null</code> if no solution exists.
         */
        private ECFieldElement solveQuadraticEquation(ECFieldElement beta)
        {
            if (beta.isZero())
            {
                return beta;
            }

            ECFieldElement zeroElement = fromBigInteger(ECConstants.ZERO);

            ECFieldElement z = null;
            ECFieldElement gamma = null;

            Random rand = new Random();
            do
            {
                ECFieldElement t = fromBigInteger(new BigInteger(m, rand));
                z = zeroElement;
                ECFieldElement w = beta;
                for (int i = 1; i <= m - 1; i++)
                {
                    ECFieldElement w2 = w.square();
                    z = z.square().add(w2.multiply(t));
                    w = w2.add(beta);
                }
                if (!w.isZero())
                {
                    return null;
                }
                gamma = z.square().add(z);
            }
            while (gamma.isZero());

            return z;
        }
        
        public boolean equals(
            Object anObject)
        {
            if (anObject == this) 
            {
                return true;
            }

            if (!(anObject instanceof ECCurve.F2m)) 
            {
                return false;
            }

            ECCurve.F2m other = (ECCurve.F2m)anObject;

            return (this.m == other.m) && (this.k1 == other.k1)
                && (this.k2 == other.k2) && (this.k3 == other.k3)
                && a.equals(other.a) && b.equals(other.b);
        }

        public int hashCode()
        {
            return this.a.hashCode() ^ this.b.hashCode() ^ m ^ k1 ^ k2 ^ k3;
        }

        public int getM()
        {
            return m;
        }

        /**
         * Return true if curve uses a Trinomial basis.
         * 
         * @return true if curve Trinomial, false otherwise.
         */
        public boolean isTrinomial()
        {
            return k2 == 0 && k3 == 0;
        }
        
        public int getK1()
        {
            return k1;
        }

        public int getK2()
        {
            return k2;
        }

        public int getK3()
        {
            return k3;
        }

        public BigInteger getN()
        {
            return n;
        }

        public BigInteger getH()
        {
            return h;
        }
    }
}
