import mpi.* ;
import java.awt.* ;
import javax.swing.* ;

//compile: javac -cp ".;$env:MPJ_HOME\lib\mpj.jar" MPJBriansBrain.java
//run: mpjrun.bat -np 4 MPJBriansBrain

public class MPJBriansBrain {
    static int P, me, B ;
    final static int N = 256 ;
    final static int CELL_SIZE = 4 ;
    final static int DELAY = 100 ;

    static int [][] state ;
    static int [][] sums ;

    static int [][] allStates ; // For display in process 0

    static Display display ;

    public static void main(String[] args) {
        MPI.Init(args) ;

        me = MPI.COMM_WORLD.Rank() ;
        P = MPI.COMM_WORLD.Size() ;
        B = N / P ;

        if (me == 0) {
            allStates = new int [N][N] ;
            display = new Display() ;
        }

        state = new int[B+2][N] ;
        sums  = new int[B+2][N] ;

        if(me == 0) {
            // Define initial state of Life board
            for(int i = 1 ; i < B+1 ; i++) {
                for(int j = 0 ; j < N ; j++) {
                    state [i] [j] = randomState() ;
                }
            }
        }

        displayStates() ;

        int begin = 1 ;
        int end = B + 1 ;

        if(me == 0) {
            begin = 2 ;
        }

        if (me == P-1) {
            end = B ;
        }

        int iter = 0 ;
        while(iter < 10000) {
            if (me == 0) {
                System.out.println("iter = " + iter++) ;
            }
            // Edge swap
            int next = (me + 1) % P ;
            int prev = (me - 1 + P) % P ;

            MPI.COMM_WORLD.Sendrecv(state [B], 0, N, MPI.INT, next, 0,
                                    state [0], 0, N, MPI.INT, prev, 0) ;
            MPI.COMM_WORLD.Sendrecv(state [1], 0, N, MPI.INT, prev, 0,
                                    state [B+1], 0, N, MPI.INT, next, 0) ;

            // Calculate neighbour sums.
            for(int i = begin ; i < end ; i++) {
                for(int j = 0 ; j < N ; j++) {
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

            // Update state of board values.
            for(int i = begin ; i < end ; i++) {
                for(int j = 0 ; j < N ; j++) {
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
            
            displayStates() ;
        }

        MPI.Finalize() ;
    }

    static Integer randomState() {
        int[] states = {0, 1, 100};
        return states[(int) (Math.random() * 3)];
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

    public static void displayStates() {
        if(me > 0) {
            MPI.COMM_WORLD.Send(state, 1, B, MPI.OBJECT, 0, 0) ;
        }
        else {  // me == 0
            for(int i = 1 ; i <= B ; i++) {
                for(int j = 0 ; j < N ; j++) {
                allStates [i - 1] [j] = state [i] [j] ;
                }
            }
            for(int src = 1 ; src < P ; src++) {
                MPI.COMM_WORLD.Recv(allStates, src * B, B, MPI.OBJECT, src, 0) ;
            }        
            display.repaint() ;
            pause() ;
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
                    if(allStates [i] [j] == 100) {
                        g.setColor(Color.WHITE) ;
                        g.fillRect(CELL_SIZE * i, CELL_SIZE * j,
                                    CELL_SIZE, CELL_SIZE) ;
                    }
                    else if(allStates [i] [j] == 1) {
                        g.setColor(Color.BLUE) ;
                        g.fillRect(CELL_SIZE * i, CELL_SIZE * j,
                                    CELL_SIZE, CELL_SIZE) ;                        
                    }
                }
            } 
        }
    }
}
