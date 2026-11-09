import mpi.* ;
import java.awt.* ;
import javax.swing.* ;

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

            // Define initial state of Life board
            for(int i = N / 4 ; i < (N * 3) / 4 ; i++) {
                for(int j = N / 4 ; j < (N * 3) / 4 ; j++) {
                    state [i] [j] = randomState() ;
                }
            }

            display = new Display() ;
            display.repaint() ;
            pause() ;
        }

        state = new int[B+2][N] ;
        sums  = new int[B+2][N] ;

        // Don't change ghost regions in update loops

        int begin = 1 ;
        int end = B + 1 ;
            
        // Don't update fixed boundary values

        if(me == 0) {
            begin = 2 ;
        }

        if (me == P-1) {
            end = B ;
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
}
