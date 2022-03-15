package com.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


public class GivenSolutionNP implements Simple_Iteration{

    public static void main(String[] args) {
        Vector<Double> Ax = new Vector<>(VectorSize);
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
            Ax = methods.mulMatrixOnVector(A,x,1,Ax);
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
    public Vector<Double> mulMatrixOnVector(List<Double> A, Vector<Double> x ,int countThreads,Vector<Double> result) {
        for (int row = 0; row < MatrixHight; row++) {
            double sumOfElements = 0;
            for (int column = 0; column < MatrixWeight; column++) {
                sumOfElements += A.get(row * MatrixWeight + column);
            }
            sumOfElements *= x.get(row);
            result.add(row, sumOfElements);
        }
        return result;
    }

    @Override
    public Vector<Double> step(Vector<Double> Ax,Vector<Double> b, Vector<Double> x) {
        Object tmpAx = Ax.clone();
        Vector<Double> tmp = Simple_Iteration.subVectorFromVector((Vector<Double>) tmpAx,b);
        Simple_Iteration.mulVectorOnConst(tmp, scale);
        tmp = Simple_Iteration.subVectorFromVector(x, tmp);
        return tmp;
    }

    @Override
    public boolean isDone(Vector<Double> Ax,Vector<Double> b) {
        Object tmpAx = Ax.clone();
        double u = Simple_Iteration.takeNorm(Simple_Iteration.subVectorFromVector((Vector<Double>) tmpAx,b));
        double v = Simple_Iteration.takeNorm(b);
        return ((u/ v)<epsilon);
    }
}
