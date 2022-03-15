package com.parallel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class GivenSolutionFP implements Simple_Iteration {
    public static final int NumberOfThreads = 2;

    public static void main(String[] args) throws InterruptedException {

        Vector<Double> Ax = new Vector<>(VectorSize);
        List<Double> A = new ArrayList<>(); //matrix
        Vector<Double> b = new Vector<>(); //vector
        Vector<Double> x = new Vector<>(); //vector

        GivenSolutionFP methods = new GivenSolutionFP();
        methods.initA(A);
        methods.initB(b);
        methods.initX(x);

        ArrayList<Boolean> tasks = new ArrayList<>();
        ArrayList<matrixThread> threads = new ArrayList<>();

        boolean flag = false;
        checkerThread checker = new checkerThread(flag,Ax,b,x,threads);
        for (int i = 0; i < NumberOfThreads; i++) {
            tasks.add(true);
            threads.add(new matrixThread(i, x, Ax, A, checker));
        }
        checker.setThreads(threads);
        for(matrixThread thread: threads){
            thread.start();
        }
        checker.start();
    }

    @Override
    public void initA(List<Double> A) {
        for (int i = 0; i < MatrixHight; i++) {
            for (int j = 0; j < MatrixWeight; j++) {
                if (i == j) {
                    A.add(2.0);
                } else {
                    A.add(1.0);
                }
            }
        }
    }

    @Override
    public void initB(Vector<Double> b) {
        for (int i = 0; i < VectorSize; i++) {
            b.add(VectorSize + 1.0);
        }
    }

    @Override
    public void initX(Vector<Double> x) {
        for (int i = 0; i < VectorSize; i++) {
            x.add((double) 10);
        }
    }

    @Override
    public Vector<Double> mulMatrixOnVector(List<Double> A, Vector<Double> x, int currThread, Vector<Double> result) {
        if(result.size()<VectorSize) {
            for (int i = 0; i < VectorSize; i++) {
                result.add((double) 0);
            }
        }
        int currRow = currThread * MatrixHight / NumberOfThreads;
        for (int i = 0; i < (MatrixHight / NumberOfThreads); currRow++, i++) {
            double sumOfElements = 0;
            for (int column = 0; column < MatrixWeight; column++) {
                sumOfElements += A.get(currRow * MatrixWeight + column);
            }
            sumOfElements *= x.get(currRow);
            result.set(currRow, sumOfElements);
        }
        return result;
    }

    @Override
    public Vector<Double> step(Vector<Double> Ax, Vector<Double> b, Vector<Double> x) {
        Object tmpAx = Ax.clone();
        Vector<Double> tmp = Simple_Iteration.subVectorFromVector((Vector<Double>) tmpAx, b);
        Simple_Iteration.mulVectorOnConst(tmp, scale);
        tmp = Simple_Iteration.subVectorFromVector(x, tmp);
        return tmp;
    }

    @Override
    public boolean isDone(Vector<Double> Ax, Vector<Double> b) {
        Object tmpAx = Ax.clone();
        double u = Simple_Iteration.takeNorm(Simple_Iteration.subVectorFromVector((Vector<Double>) tmpAx, b));
        double v = Simple_Iteration.takeNorm(b);
        return ((u / v) < epsilon);
    }
}
    class matrixThread extends GivenSolutionFP implements Runnable {
        public Thread t;
        boolean suspended = false;
        boolean over = false;
        int currThread;
        Vector<Double> x;
        Vector<Double> Ax;
        List<Double> A;
        checkerThread checker;

        matrixThread(int currThread,Vector<Double> x,Vector<Double> Ax,List<Double> A,checkerThread checker) {
            this.currThread = currThread;
            this.x=x;
            this.A=A;
            this.Ax=Ax;
            this.checker = checker;
            System.out.println("Создание " + currThread);
        }

        public void run() {
            //System.out.println("Выолнение " + currThread);
            try {
                while (!over) {
                    //System.out.println("Поток: " + currThread);
                    mulMatrixOnVector(A,x,currThread,Ax);
                    suspended = true;
                    synchronized (this) {
                        while (suspended) {
                            checker.resume();
                            wait();
                        }
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Поток " + currThread + " прерван.");
            }
            System.out.println("Поток " + currThread + " завершается.");
        }

        public void start() {
            System.out.println("Запуск " + currThread);
            if (t == null) {
                t = new Thread(this, String.valueOf(currThread));
                t.start();
            }
        }
        public synchronized void resume() {

            suspended = false;
            notify();
        }
        public  synchronized void over(){
            over = true;
            suspended = false;
            notify();
        }
    }

    class checkerThread extends GivenSolutionFP implements Runnable {
        long startTime = System.nanoTime();
        public Thread t;
        int countResume = 0;
        boolean flag;
        boolean suspended = false;
        Vector<Double> Ax;
        Vector<Double> b;
        Vector<Double> x;
        ArrayList<matrixThread> threads;
        boolean firstStep = true;

        checkerThread(boolean flag,Vector<Double> Ax, Vector<Double> b,Vector<Double> x,ArrayList<matrixThread> threads) {
            this.flag = flag;
            this.Ax = Ax;
            this.b = b;
            this.x = x;
            this.threads = threads;
            System.out.println("Создание checkerThread");
        }

        public void run() {
            try {
                while (!flag) {
                    synchronized (this) {
                        while (firstStep) {
                            wait();
                        }
                    }
                    //System.out.println("Выолнение checkerThread");
                    suspended = true;
                    flag = isDone(Ax, b);
                    x = step(Ax, b, x);
                    //System.out.println(x);
                    suspended = true;
                    synchronized (this) {
                        while (suspended) {
                            for (matrixThread thread : threads) {
                                thread.resume();
                            }
                            wait();
                        }
                    }
                }

            } catch (InterruptedException e) {
                System.out.println("checkerThread прерван.");
            }
            for (matrixThread thread : threads) {
                thread.over();
            }
            long elapsedNanos = System.nanoTime() - startTime;
            System.out.println(elapsedNanos / 1000000000.0);
            System.out.println("checkerThread завершается.");
        }

        public void start() {
            //System.out.println("Запуск checkerThread");
            if (t == null) {
                t = new Thread(this, "checkerThread");
                t.start();
            }
        }
        public synchronized void resume() {
            countResume++;
            if(countResume == NumberOfThreads) {
                suspended = false;
                firstStep = false;
                notify();
                countResume = 0;
            }
        }

        public boolean getFlag(){
            return flag;
        }

        public void setThreads(ArrayList<matrixThread> threads) {
            this.threads = threads;
        }
    }


