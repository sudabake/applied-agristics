package io.github.sudabake.appliedagristics.logic;

import java.util.function.DoubleSupplier;

/** Exact binomial sampling by beta splitting; work grows logarithmically with parallel count. */
public final class SeedReproduction {
    public static final double FALLBACK_CHANCE=.10;
    public static long roll(long cycles,DoubleSupplier uniform,DoubleSupplier gaussian) {
        return binomial(cycles,FALLBACK_CHANCE,uniform,gaussian);
    }
    private static long binomial(long n,double p,DoubleSupplier u,DoubleSupplier g) {
        long result=0;
        while (n>0 && p>0) {
            if (p>=1) return result+n;
            if (n*p<16) {
                // Geometric waiting times: one draw per success, never one per occupied seed.
                double logFailure=Math.log1p(-p); long position=0;
                while (position<n) {
                    double gap=Math.floor(Math.log(Math.max(Double.MIN_VALUE,u.getAsDouble()))/logFailure);
                    if (gap>=n-position) break;
                    position+=(long)gap+1; result++;
                }
                return result;
            }
            long a=1+n/2,b=n-a+1;
            double x=gamma(a,u,g),y=gamma(b,u,g),split=x/(x+y);
            if (split>=p) { n=a-1; p/=split; }
            else { result+=a; n=b-1; p=(p-split)/(1-split); }
        }
        return result;
    }
    private static double gamma(double shape,DoubleSupplier u,DoubleSupplier g) {
        double d=shape-1.0/3,c=1/Math.sqrt(9*d);
        while (true) {
            double x=g.getAsDouble(),v=1+c*x; if (v<=0) continue;
            v=v*v*v; double sample=u.getAsDouble(),xx=x*x;
            if (sample<1-.0331*xx*xx || Math.log(sample)<.5*xx+d*(1-v+Math.log(v))) return d*v;
        }
    }
    private SeedReproduction() {}
}
