package com.parallel;

import java.util.List;
import java.util.Vector;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

/**
 * @logic f(x+1)=f(x)-scale(Af(x)-b)
 * @end if u/v < epsilon then
 *      u = norm(Ax-b)
 *      v = norm(b)
 *
 */
public abstract class Simple_Iteration {
    static int VectorSize = 8192;
    static int MatrixHight = 8192;
    static int MatrixWeight = 8192;
    double epsilon = 0.000001;
    double scale = epsilon*10;
    abstract void initA(List<Double> A);
    abstract void initB(Vector<Double> b);
    abstract void initX(Vector<Double> x);
    abstract List<Double> mulMatrixOnVector(List<Double> A, Vector<Double> x, int countThreads,Vector<Double> result);
    static double takeNorm(Vector<Double> vector){
        double sum=0;
        for(int i=0;i<VectorSize;i++){
            sum+=vector.get(i);
        }
        return sqrt(abs(sum));
    }
    static void mulVectorOnConst(Vector<Double> x, double t) {
        for(int row=0;row<VectorSize;row++){
            x.set(row,x.get(row)*t);
        }
    }
    static Vector<Double> subVectorFromVector(Vector<Double> x, Vector<Double> y){
        for(int row=0;row<VectorSize;row++){
            x.set(row,x.get(row)-y.get(row));
        }
        return x;
    }

    /**
     * @return Performs the next calculation step according to the simple iteration formula
     */
    abstract Vector<Double> step(Vector<Double> Ax,Vector<Double> b,Vector<Double> x);
    /**
     * @return returns true if the calculation resulted in an answer
     */
    abstract boolean isDone(Vector<Double> Ax,Vector<Double> b);
}
