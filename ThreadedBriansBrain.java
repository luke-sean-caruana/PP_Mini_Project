import java.awt.* ;
import javax.swing.* ;

import java.util.Random;
import java.util.Scanner;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class ThreadedBriansBrain extends Thread {

    final static int P = 32 ;          // Number of threads
    final static int N = 1024 ;
    final static int CELL_SIZE = 1 ;
    final static int DELAY = 100 ;

    static boolean BENCH;
    static long SEED;
    final static int ITERATIONS = 1000;
    static Random rng;

    // Shared by all P workers
    final static CyclicBarrier barrier = new CyclicBarrier(P);

    static int [][] state = new int [N][N] ;
    static int [][] sums  = new int [N][N] ;

    static Display display;


    public static void main(String args []) throws Exception {

        Scanner input = new Scanner(System.in);
        String choice = "";

        while (!choice.equals("b") && !choice.equals("v")) {
            System.out.print("Bench or visual? (b/v): ");
            choice = input.nextLine();
        }

        BENCH = choice.equals("b");
        SEED = BENCH ? 1L : System.nanoTime();
        rng  = new Random(SEED);

        input.close();


        if (!BENCH) {
            display = new Display();
        }

        // Define initial state of board
        for (int i = 0 ; i < N ; i++) {
            for (int j = 0 ; j < N ; j++) {
                state [i] [j] = randomState() ;
            }
        }

        System.out.println("seed = " + SEED);

        if (!BENCH) {
            display.repaint() ;
            pause() ;
        }

        long start = System.nanoTime();

        ThreadedBriansBrain [] threads = new ThreadedBriansBrain[P] ;
        for (int me = 0 ; me < P ; me++) {
            threads [me] = new ThreadedBriansBrain(me) ;
            threads [me].start() ;
        }

        for (int me = 0 ; me < P ; me++) {
            threads [me].join() ;
        }

        if (BENCH) {
            double seconds = (System.nanoTime() - start) / 1e9;
            System.out.println("P = " + P + ", " + ITERATIONS + " iterations: " + seconds + " s");
        }
    }

    static class Display extends JPanel {

        final static int WINDOW_SIZE = N * CELL_SIZE ;

        Display() {

            setPreferredSize(new Dimension(WINDOW_SIZE, WINDOW_SIZE));

            JFrame frame = new JFrame("Brian's Brain");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(this);
            frame.pack();
            frame.setVisible(true);
        }

        public void paintComponent(Graphics g) {
            g.setColor(Color.BLACK) ;
            g.fillRect(0, 0, WINDOW_SIZE, WINDOW_SIZE) ;
            for (int i = 0 ; i < N ; i++) {
                for (int j = 0 ; j < N ; j++) {
                    if (state [i] [j] == 100) {
                        g.setColor(Color.WHITE) ;
                        g.fillRect(CELL_SIZE * i, CELL_SIZE * j,
                                   CELL_SIZE, CELL_SIZE) ;
                    }
                    else if (state [i] [j] == 1) {
                        g.setColor(Color.BLUE) ;
                        g.fillRect(CELL_SIZE * i, CELL_SIZE * j,
                                   CELL_SIZE, CELL_SIZE) ;
                    }
                }
            }
        }
    }

    static void pause() {
        try {
            Thread.sleep(DELAY) ;
        }
        catch (InterruptedException e) {
            e.printStackTrace() ;
            System.exit(1) ;
        }
    }

    static void synch() {
        try {
            barrier.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (BrokenBarrierException e) {
            // barrier broken: just return
        }
    }

    static int randomState() {
        int[] states = {0, 1, 100};
        return states[rng.nextInt(3)];
    }

    int me ;

    public ThreadedBriansBrain(int me) {
        this.me = me ;
    }

    public void run() {

        int begin = me * N / P;
        int end   = (me + 1) * N / P;

        for (int iter = 0; !BENCH || iter < ITERATIONS; iter++) {

            // Calculate neighbour sums.
            for (int i = begin ; i < end ; i++) {
                for (int j = 0 ; j < N ; j++) {

                    int ip = (i + 1) % N ;
                    int im = (i - 1 + N) % N ;
                    int jp = (j + 1) % N ;
                    int jm = (j - 1 + N) % N ;

                    sums [i] [j] =
                        state [im] [jm] + state [im] [ j] + state [im] [jp] +
                        state [ i] [jm]                   + state [ i] [jp] +
                        state [ip] [jm] + state [ip] [ j] + state [ip] [jp] ;
                }
            }
            synch();

            // Update state of board values.
            for (int i = begin ; i < end ; i++) {
                for (int j = 0 ; j < N ; j++) {
                    if (state [i] [j] == 100) {
                        state [i] [j] = 1 ;
                    }
                    else if (state [i] [j] == 1) {
                        state [i] [j] = 0 ;
                    }
                    else if (sums [i] [j] >= 200 && sums [i] [j] < 300) {
                        state [i] [j] = 100 ;
                    }
                }
            }
            synch();

            if (!BENCH && me == 0) {
                System.out.println("iter = " + iter);
                display.repaint();
                pause();
            }
        }
    }
}