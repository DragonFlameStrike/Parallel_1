package com.parallel;



import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class GivenSolutionSP extends Simple_Iteration {
    public static final int NumberOfThreads = 16;

    public static void main(String[] args) throws InterruptedException {
        for(int j=0;j<3;j++) {
            System.out.println();
            System.out.println();

            ArrayList<Boolean> tasks = new ArrayList<>();
            ArrayList<workerThreadSecond> workers = new ArrayList<>();

            boolean flag = false;
            /*
             * Init Checker and Worker threads
             */
            checkerThreadSecond checker = new checkerThreadSecond(flag, workers);
            for (int currThread = 0; currThread < NumberOfThreads; currThread++) {
                tasks.add(true);

                workers.add(new workerThreadSecond(currThread, checker));
            }
            /*
             *  Connect Checker and Workers to use wait and notify
             */
            checker.setThreads(workers);
            for (workerThreadSecond worker : workers) {
                worker.start();
            }
            checker.start();
            while(true){
                if(checker.getFlag()){
                    break;
                }
                else Thread.sleep(10);
            }
            Thread.sleep(100);
        }
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
        for (int i = 0; i < VectorSize/NumberOfThreads; i++) {
            x.add((double) 10);
        }
    }
    public void initCheckerX(Vector<Double> x){
        for (int i = 0; i < VectorSize; i++) {
            x.add((double) 10);
        }
    }

    @Override
    public Vector<Double> mulMatrixOnVector(List<Double> partOfA, Vector<Double> x, int currThread, Vector<Double> result) {

        int currRow = currThread * MatrixHight / NumberOfThreads;
        for (int i = 0; i < (MatrixHight / NumberOfThreads); currRow++, i++) {
            double sumOfElements = 0;
            for (int column = 0; column < MatrixWeight; column++) {
                sumOfElements += partOfA.get(i * MatrixWeight + column) * x.get(i);
            }
            result.set(currRow, sumOfElements);
        }
        return result;
    }

    @Override
    public Vector<Double> step(Vector<Double> Ax, Vector<Double> b, Vector<Double> x) {
        Object tmpAx = Ax.clone();
        Vector<Double> tmp = Simple_Iteration.subVectors((Vector<Double>) tmpAx, b);
        Simple_Iteration.mulVectorOnConst(tmp, scale);
        tmp = Simple_Iteration.subVectors(x, tmp);
        return tmp;
    }

    @Override
    public boolean isDone(Vector<Double> Ax, Vector<Double> b) {
        Object tmpAx = Ax.clone();
        double u = Simple_Iteration.takeNorm(Simple_Iteration.subVectors((Vector<Double>) tmpAx, b));
        double v = Simple_Iteration.takeNorm(b);
        return ((u / v) < epsilon);
    }
}
class workerThreadSecond extends GivenSolutionSP implements Runnable {
    public Thread t;
    boolean suspended = false;
    boolean over = false;
    int currThread;
    double totalTime = 0;
    Vector<Double> partOfX = new Vector<>();
    Vector<Double> Ax = new Vector<>();
    List<Double> transpPartOfA;
    checkerThreadSecond checker;
    double workerTime;

    workerThreadSecond(int currThread, checkerThreadSecond checker) {
        this.currThread = currThread;
        initX(this.partOfX);
        transpPartOfA = initA();
        for (int i = 0; i < VectorSize; i++) Ax.add((double) 0);
        this.checker = checker;
        //System.out.println("Создание " + currThread);
    }

    public void run() {
        //System.out.println("Выолнение " + currThread);
        try {
            while (!over) {
                //System.out.println("Поток: " + currThread);
                long startTime = System.nanoTime();
                for (int i = 0; i < VectorSize/NumberOfThreads; i++) partOfX.set(i, checker.getXbyIndex(i+currThread*VectorSize/NumberOfThreads));
                mulMatrixOnVector(transpPartOfA, partOfX, currThread, Ax);

                suspended = true;
                //System.out.println("Поток "+currThread+" завершил работу за "+(System.nanoTime()-startTime)/1000000000.0);

                synchronized (this) {
                    while (suspended) {
                        checker.resume();
                        workerTime = (System.nanoTime() - startTime) / 1000000000.0;
                        totalTime += workerTime;
                        wait();
                    }
                }

            }
        } catch (InterruptedException e) {
            System.out.println("Поток " + currThread + " прерван.");
        }
        //System.out.println("Поток " + currThread + " завершается.");
    }

    public void start() {
        //System.out.println("Запуск " + currThread);
        if (t == null) {
            t = new Thread(this, String.valueOf(currThread));
            t.start();
        }
    }

    public synchronized void resume() {

        suspended = false;
        notifyAll();
    }

    public synchronized void over() {
        over = true;
        suspended = false;
        notify();
    }

    private List<Double> initA() {
        List<Double> A = new ArrayList<>();
        for (int i = 0; i < MatrixHight; i++) {
            for (int j = 0; j < MatrixWeight; j++) {
                if ((i * MatrixWeight + j) >= currThread * MatrixWeight * MatrixHight / NumberOfThreads) {
                    if ((i * MatrixWeight + j) < (currThread + 1) * MatrixWeight * MatrixHight / NumberOfThreads) {
                        if (i == j) {
                            A.add(2.0);
                        } else {
                            A.add(1.0);
                        }
                    }
                }
            }
        }
        return A;
    }

    public Vector<Double> getAx() {
        return Ax;
    }

    public double getTotalTime() {
        return totalTime;
    }

    public double getWorkerTime() {
        return workerTime;
    }
}
class checkerThreadSecond extends GivenSolutionSP implements Runnable {
    long startTime = System.nanoTime();
    public Thread t;
    int countResume = 0;
    boolean flag;
    boolean suspended = false;
    Vector<Double> Ax;
    Vector<Double> b = new Vector<>();
    Vector<Double> x = new Vector<>();
    ArrayList<workerThreadSecond> threads;
    boolean firstStep = true;
    double totalTime = 0;
    double checkerMaxTime = 0;

    checkerThreadSecond(boolean flag, ArrayList<workerThreadSecond> threads) {
        this.flag = flag;
        Ax = new Vector<Double>();
        for (int i = 0; i < VectorSize; i++) Ax.add((double) 0);
        initB(b);
        initCheckerX(x);
        this.threads = threads;
        //System.out.println("Создание checkerThread");
    }

    public void run() {
        try {
            while (!flag) {
                synchronized (this) {
                    while (firstStep) {
                        wait();
                    }
                }
                long time = System.nanoTime();
                double maxTime = 0;
                for (workerThreadSecond thread : threads) {
                    if (thread.getWorkerTime() > maxTime) maxTime = thread.getWorkerTime();
                }
                checkerMaxTime += maxTime;
                //System.out.println("Выолнение checkerThread");
                suspended = true;
                for (int i = 0; i < VectorSize; i++) Ax.set(i, (double) 0);
                for (workerThreadSecond thread : threads) {
                    Vector<Double> tempAx = thread.getAx();
                    sumVectors(Ax, tempAx);
                }
                flag = isDone(Ax, b);
                x = step(Ax, b, x);
                //System.out.println(x);
                suspended = true;

                synchronized (this) {
                    while (suspended) {
                        for (workerThreadSecond thread : threads) {
                            thread.resume();
                        }
                        totalTime += (System.nanoTime() - time) / 1000000000.0;
                        wait();
                    }
                }

            }

        } catch (InterruptedException e) {
            //System.out.println("checkerThread прерван.");
        }
        Vector<Double> workersTime = new Vector<>();
        double workersTotalTime = 0;
        for (workerThreadSecond thread : threads) {
            double tmp = thread.getTotalTime();
            workersTime.add(tmp);
            workersTotalTime += tmp;
        }
        System.out.println("checker total time - " + totalTime);
        System.out.println("workers time - "+ workersTime);
        System.out.println("sum of workers time - " + workersTotalTime);
        System.out.println("sum of max workers ticks - " + checkerMaxTime);
        for (workerThreadSecond thread : threads) {
            thread.over();
        }
        long elapsedNanos = System.nanoTime() - startTime;
        System.out.println("Total time - " + elapsedNanos / 1000000000.0);
        //System.out.println("checkerThread завершается.");
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
        if (countResume == NumberOfThreads) {
            suspended = false;
            firstStep = false;
            notify();
            countResume = 0;
        }
    }

    public boolean getFlag() {
        return flag;
    }

    public void setThreads(ArrayList<workerThreadSecond> threads) {
        this.threads = threads;
    }

    public Vector<Double> getX() {
        return x;
    }

    public double getXbyIndex(int i) {
        return x.get(i);
    }
}


