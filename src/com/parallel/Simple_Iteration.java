package com.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
public interface Simple_Iteration {
    int VectorSize = 500;
    int MatrixHight = 500;
    int MatrixWeight = 500;
    double epsilon = 0.000001;
    double scale = epsilon*10;
    void initA(List<Double> A);
    void initB(Vector<Double> b);
    void initX(Vector<Double> x);
    List<Double> mulMatrixOnVector(List<Double> A, Vector<Double> x);
    double takeNorm(Vector<Double> vector);
    Vector<Double> mulVectorOnConst(Vector<Double> x, double t);
    Vector<Double> subVectorFromVector(Vector<Double> x, Vector<Double> y);

    /**
     * @return Performs the next calculation step according to the simple iteration formula
     */
    Vector<Double> step(Vector<Double> Ax,Vector<Double> b,Vector<Double> x);
    /**
     * @return returns true if the calculation resulted in an answer
     */
    boolean isDone(Vector<Double> Ax,Vector<Double> b);
}
