package classExamples.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrimeNumGen extends JFrame
{

    private final JTextArea aTextField = new JTextArea();
    private final JButton primeButton = new JButton("Start");
    private final JButton cancelButton = new JButton("Cancel");
    private volatile boolean cancel = false;
    private final PrimeNumGen thisFrame;

    public static void main(String[] args)
    {
        PrimeNumGen png = new PrimeNumGen("Primer Number Generator");

        // don't add the action listener from the constructor
        png.addActionListeners();
        png.setVisible(true);

    }

    private PrimeNumGen(String title)
    {
        super(title);
        this.thisFrame = this;
        cancelButton.setEnabled(false);
        aTextField.setEditable(false);
        setSize(200, 200);
        setLocationRelativeTo(null);
        //kill java VM on exit
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(primeButton,  BorderLayout.SOUTH);
        getContentPane().add(cancelButton,  BorderLayout.EAST);
        getContentPane().add( new JScrollPane(aTextField),  BorderLayout.CENTER);
    }

    private class CancelOption implements ActionListener
    {
        public void actionPerformed(ActionEvent arg0)
        {
            cancel = true;
        }
    }

    private void addActionListeners()
    {
        cancelButton.addActionListener(new CancelOption());

        primeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {

                String num = JOptionPane.showInputDialog("Enter First Number");
                Integer max =null;

                try
                {
                    max = Integer.parseInt(num);
                }
                catch(Exception ex)
                {
                    JOptionPane.showMessageDialog(
                            thisFrame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }

                if( max != null)
                {
                    aTextField.setText("");
                    primeButton.setEnabled(false);
                    cancelButton.setEnabled(true);
                    cancel = false;
                    new Thread(new UserInput(max)).start();

                }
            }});
    }

    private boolean isPrime( int i)
    {
        for( int x=2; x < i -1; x++)
            if( i % x == 0  )
                return false;

        return true;
    }

    private class UserInput implements Runnable
    {
        private final int max;
        long startTime =  System.currentTimeMillis();

        private UserInput(int num)
        {
            this.max = num;
        }

        public void run()
        {
            long lastUpdate = System.currentTimeMillis();
            List<Integer> list = new ArrayList<Integer>();

            // check the number of processors available
            int numProcessors = Runtime.getRuntime().availableProcessors() + 1;

            // create a thread pool of size equal to numofprocessors
            ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(numProcessors);

            System.out.println("Number of processors " +  numProcessors);

            for (int i = 1; i < max && ! cancel; i++)
            {
                // check if a number is prime or not in a new thread. We are trying to parallelize the work here
                threadPoolExecutor.execute(new isPrimeWorker(i, list, lastUpdate, max));
            }

            threadPoolExecutor.shutdown();
            while (!threadPoolExecutor.isTerminated()) {   }


            final StringBuffer buff = new StringBuffer();


            // sort the list
            list.removeAll(Collections.singleton(null));
            Collections.sort(list);
            for( Integer i2 : list)
                buff.append(i2 + "\n");

            if( cancel)
                buff.append("cancelled");

            SwingUtilities.invokeLater( new Runnable()
            {
                @Override
                public void run()
                {
                    cancel = false;
                    primeButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                    aTextField.setText( (cancel ? "cancelled " : "") +  buff.toString());
                }
            });

            long endTime = System.currentTimeMillis();

            long timeElapsed = (endTime - startTime);

            System.out.println("Time taken in milliseconds " + timeElapsed);
            System.out.println("Max number is " + max);
            System.out.println("===============================================" + "\n");


        }// end run

    }  // end UserInput


    // Runnable class to check if a number is prime
    // this code is taken from the UserInput run method
    private class isPrimeWorker implements Runnable {

        List<Integer> list;
        int i;
        Long lastUpdate;
        int max;

        public isPrimeWorker(int number, List<Integer> list, Long lastUpdate, int max) {
            this.list = list;
            this.i = number;
            this.lastUpdate = lastUpdate;
            this.max = max;
        }

        @Override
        public void run() {


            if (isPrime(i)) {
                // Ideally  this part should be synchronized but I dont see any race conditions here

                list.add(i);

                // This logic also can be wrong at times, we need to test thoroughly if this logic still works

                if (System.currentTimeMillis() - lastUpdate > 500) {
                    final String outString = "Found " + list.size() + " in " + i + " of " + max;

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            aTextField.setText(outString);
                        }
                    });

                    lastUpdate = System.currentTimeMillis();
                }
            }


        }
    }


}