import java.awt.* ;
import javax.swing.* ;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class ThreadedSquaresBriansBrain extends Thread {

    final static int P = 4 ; // Number of threads. NOTE: Only works for 4 so far. Should work for any perfect square.
    final static int N = 256 ;
    final static int CELL_SIZE = 4 ;
    final static int DELAY = 100 ;

    // Shared by all P workers
    final static CyclicBarrier barrier = new CyclicBarrier(P);

    static int [][] state = new int [N][N] ;
    static int [][] sums  = new int [N][N] ;

    static Display display = new Display() ;
  
    public static void main(String args []) throws Exception {
  
        // Define initial state of Life board
        for(int i = N / 4 ; i < (N * 3) / 4 ; i++) {
            for(int j = N / 4 ; j < (N * 3) / 4 ; j++) {
                state [i] [j] = randomState() ;
            }
        }
  
        display.repaint() ;
        pause() ;

        ThreadedSquaresBriansBrain [] threads = new ThreadedSquaresBriansBrain[P] ;
        for(int me = 0 ; me < P ; me++) {
            threads [me] = new ThreadedSquaresBriansBrain(me) ;
            threads [me].start() ;
        }

        for(int me = 0 ; me < P ; me++) {
            threads [me].join() ;
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
            for(int i = 0 ; i < N ; i++) {
                for(int j = 0 ; j < N ; j++) {
                    if(state [i] [j] == 100) {
                        g.setColor(Color.WHITE) ;
                        g.fillRect(CELL_SIZE * i, CELL_SIZE * j,
                                    CELL_SIZE, CELL_SIZE) ;
                    }
                    else if(state [i] [j] == 1) {
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
        catch(InterruptedException e) {
            e.printStackTrace() ;
            System.exit(1) ;
        }
    }

    static void synch() {
        try {
                barrier.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (BrokenBarrierException e) {
                return;
            }
    }
    static Integer randomState() {
        int[] states = {0, 1, 100};
        return states[(int) (Math.random() * 3)];
    }

    int me ;

    public ThreadedSquaresBriansBrain(int me) {
        this.me = me ;
    }

    int iter = 0 ;
    public void run() {
        while(true) {

            if (me == 0) {
                System.out.println("iter = " + iter++) ;
            }

            int iBegin = (me / (int) Math.sqrt(P)) * (N / (int) Math.sqrt(P));
            int iEnd = iBegin + N / (int) Math.sqrt(P);

            int jBegin = (me % (int) Math.sqrt(P)) * (N / (int) Math.sqrt(P));
            int jEnd = jBegin + N / (int) Math.sqrt(P);

            // Calculate neighbour sums.

            for(int i = iBegin ; i < iEnd ; i++) {
                for(int j = jBegin ; j < jEnd ; j++) {
                    // find neighbours...
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
            for(int i = iBegin ; i < iEnd ; i++) {
                for(int j = jBegin ; j < jEnd ; j++) {
                    if (state [i] [j] == 100) {
                        state [i] [j] = 1 ;
                    }
                    else if (state [i] [j] == 1) {
                        state [i] [j] = 0 ;
                    }

                    else if (((sums [i] [j] >= 200) && (sums [i] [j] < 300)) && (state [i] [j] == 0)) {
                        state [i] [j] = 100 ;
                    } 
                }
            }
            synch();

            if (me == 0) {
                display.repaint() ;
                pause() ;
            }
        }
    }
}