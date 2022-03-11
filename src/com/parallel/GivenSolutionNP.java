package com.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static java.lang.Math.*;

public class GivenSolutionNP implements Simple_Iteration{

    public static void main(String[] args) {
        Vector<Double> Ax;
        List<Double> A = new ArrayList<>(); //matrix
        Vector<Double> b = new Vector<>(); //vector
        Vector<Double> x = new Vector<>(); //vector

        GivenSolutionNP methods = new GivenSolutionNP();
        methods.initA(A);
        methods.initB(b);
        methods.initX(x);
        boolean flag;
        long startTime = System.nanoTime();
        do {
            Ax = methods.mulMatrixOnVector(A,x);
            flag = methods.isDone(Ax,b);
            x = methods.step(Ax,b,x);
            //System.out.println(x);
        } while (!flag);
        long elapsedNanos = System.nanoTime() - startTime;
        System.out.println(elapsedNanos/1000000000.0);
    }

    @Override
    public void initA(List<Double> A) {
        for(int i=0;i<MatrixHight;i++){
            for(int j=0;j<MatrixWeight;j++){
                if(i==j){
                    A.add(2.0);
                }
                else{
                    A.add(1.0);
                }
            }
        }
    }

    @Override
    public void initB(Vector<Double> b) {
        for(int i=0;i<VectorSize;i++){
            b.add(VectorSize+1.0);
        }
    }

    @Override
    public void initX(Vector<Double> x) {
        for(int i=0;i<VectorSize;i++){
            x.add((double) 10);
        }
    }
    @Override
    public Vector<Double> mulMatrixOnVector(List<Double> A, Vector<Double> x) {
        Vector<Double> Ax = new Vector<>(VectorSize);
        for (int row = 0; row < MatrixHight; row++) {
            double sumOfElements = 0;
            for (int column = 0; column < MatrixWeight; column++) {
                sumOfElements += A.get(row * MatrixWeight + column);
            }
            sumOfElements *= x.get(row);
            Ax.add(row, sumOfElements);
        }
        return Ax;
    }

    @Override
    public Vector<Double> step(Vector<Double> Ax,Vector<Double> b, Vector<Double> x) {
        Object tmpAx = Ax.clone();
        return subVectorFromVector(x,mulVectorOnConst(subVectorFromVector((Vector<Double>) tmpAx,b),scale));
    }



    @Override
    public boolean isDone(Vector<Double> Ax,Vector<Double> b) {
        Object tmpAx = Ax.clone();
        return (
                (takeNorm(subVectorFromVector((Vector<Double>) tmpAx,b))/takeNorm(b))<epsilon
        );
    }

    @Override
    public double takeNorm(Vector<Double> vector) {
        double sum=0;
        for(int i=0;i<VectorSize;i++){
            sum+=vector.get(i);
        }
        return sqrt(abs(sum));
    }

    @Override
    public  Vector<Double> mulVectorOnConst(Vector<Double> x, double t) {
        for(int row=0;row<VectorSize;row++){
            x.set(row,x.get(row)*t);
        }
        return x;
    }

    @Override
    public Vector<Double> subVectorFromVector(Vector<Double> x, Vector<Double> y) {
        for(int row=0;row<VectorSize;row++){
           x.set(row,x.get(row)-y.get(row));
        }
        return x;
    }
}
