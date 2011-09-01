/**
 * 
 */
package plugins.stef.demo.benchmark;

import icy.gui.component.ComponentUtil;
import icy.gui.frame.IcyFrameAdapter;
import icy.gui.frame.IcyFrameEvent;
import icy.gui.frame.TitledFrame;
import icy.gui.util.GuiUtil;
import icy.image.IcyBufferedImage;
import icy.math.FPSMeter;
import icy.plugin.abstract_.Plugin;
import icy.plugin.interface_.PluginImageAnalysis;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;
import icy.type.TypeUtil;
import icy.type.collection.array.Array1DUtil;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 * @author Stephane
 */
public class SmoothFilterBenchmark extends Plugin implements PluginImageAnalysis
{
    private class Processor implements Runnable
    {
        private final FPSMeter fpsMeter;
        private double[] dstBuffer;

        public Processor()
        {
            super();

            fpsMeter = new FPSMeter();
            dstBuffer = null;
        }

        @Override
        public void run()
        {
            while (!end)
            {
                if (!paused)
                {
                    final IcyBufferedImage image = imgIn;
                    final String method = group.getSelection().getActionCommand();

                    if ((image != null) && (image.getDataType() == TypeUtil.TYPE_BYTE))
                    {
                        final double nbSample = image.getSizeX() * image.getSizeY();
                        final long startTime = System.nanoTime();

                        for (int c = 0; c < image.getSizeC(); c++)
                        {

                            if (method.equals(METHOD1))
                                doMethod1(image, c);
                            else if (method.equals(METHOD2))
                                doMethod2(image, c);
                            else if (method.equals(METHOD3))
                                doMethod3(image, c);
                            else if (method.equals(METHOD4))
                                doMethod4(image, c);
                            else if (method.equals(METHOD5))
                                doMethod5(image, c);
                        }

                        final double elapsedTime = System.nanoTime() - startTime;
                        fpsMeter.update();
                        double sampleRate = (nbSample / elapsedTime) * 1000;
                        label.setText("Frame process time : "
                                + String.format("%.2f", Float.valueOf((float) (elapsedTime / 1000000))) + " ms ("
                                + fpsMeter.getFPS() + " FPS) --> "
                                + String.format("%.2f", Float.valueOf((float) sampleRate)) + " Mega pixel/sec");
                    }
                }
                else
                    ThreadUtil.sleep(10);
            }

            System.out.println("exiting...");
        }

        /**
         * Per pixel access, generic data type
         */
        private void doMethod1(IcyBufferedImage image, int component)
        {
            final IcyBufferedImage src = image.getCopy();
            final IcyBufferedImage dst = image;
            final int sizeX = image.getSizeX();
            final int sizeY = image.getSizeY();

            dst.beginUpdate();
            try
            {
                for (int x = 1; x < sizeX - 1; x++)
                {
                    for (int y = 1; y < sizeY - 1; y++)
                    {
                        double value;

                        value = src.getData(x - 1, y - 1, component);
                        value += src.getData(x, y - 1, component);
                        value += src.getData(x + 1, y - 1, component);
                        value += src.getData(x - 1, y, component);
                        value += src.getData(x, y, component);
                        value += src.getData(x + 1, y, component);
                        value += src.getData(x - 1, y + 1, component);
                        value += src.getData(x, y + 1, component);
                        value += src.getData(x + 1, y + 1, component);
                        value /= 9;

                        dst.setData(x, y, component, value);
                    }
                }
            }
            finally
            {
                dst.endUpdate();
            }
        }

        /**
         * Per pixel access, fixed data type
         */
        private void doMethod2(IcyBufferedImage image, int component)
        {
            final IcyBufferedImage src = image.getCopy();
            final IcyBufferedImage dst = image;
            final int sizeX = image.getSizeX();
            final int sizeY = image.getSizeY();

            dst.beginUpdate();
            try
            {
                for (int x = 1; x < sizeX - 1; x++)
                {
                    for (int y = 1; y < sizeY - 1; y++)
                    {
                        int value;

                        value = src.getDataAsByte(x - 1, y - 1, component) & 0xFF;
                        value += src.getDataAsByte(x, y - 1, component) & 0xFF;
                        value += src.getDataAsByte(x + 1, y - 1, component) & 0xFF;
                        value += src.getDataAsByte(x - 1, y, component) & 0xFF;
                        value += src.getDataAsByte(x, y, component) & 0xFF;
                        value += src.getDataAsByte(x + 1, y, component) & 0xFF;
                        value += src.getDataAsByte(x - 1, y + 1, component) & 0xFF;
                        value += src.getDataAsByte(x, y + 1, component) & 0xFF;
                        value += src.getDataAsByte(x + 1, y + 1, component) & 0xFF;
                        value /= 9;

                        dst.setDataAsByte(x, y, component, (byte) value);
                    }
                }
            }
            finally
            {
                dst.endUpdate();
            }
        }

        /**
         * Per band access, generic data type
         */
        private void doMethod3(IcyBufferedImage image, int component)
        {
            final boolean signed = image.isSignedDataType();
            final double[] src = Array1DUtil.arrayToDoubleArray(image.getDataXY(component), signed);
            final int len = src.length;

            // rebuild dst buffer if needed
            if ((dstBuffer == null) || (dstBuffer.length != len))
                dstBuffer = new double[len];

            final int sizeX = image.getSizeX();
            final int sizeY = image.getSizeY();

            int offset = sizeX + 1;
            for (int y = 1; y < sizeY - 1; y++)
            {
                for (int x = 1; x < sizeX - 1; x++)
                {
                    double value;

                    value = src[offset - (sizeX + 1)];
                    value += src[offset - sizeX];
                    value += src[offset - (sizeX - 1)];
                    value += src[offset - 1];
                    value += src[offset];
                    value += src[offset + 1];
                    value += src[offset + (sizeX - 1)];
                    value += src[offset + sizeX];
                    value += src[offset + (sizeX + 1)];
                    value /= 9;

                    dstBuffer[offset] = value;
                    offset++;
                }

                offset += 2;
            }

            // convert and write back to original data type
            Array1DUtil.doubleArrayToArray(dstBuffer, image.getDataXY(component));

            // notify data changed
            image.dataChanged();
        }

        /**
         * Per pixel access, fixed data type
         */
        private void doMethod4(IcyBufferedImage image, int component)
        {
            final byte[] src = image.getDataCopyXYAsByte(component);
            final byte[] dst = image.getDataXYAsByte(component);

            final int sizeX = image.getSizeX();
            final int sizeY = image.getSizeY();

            int offset = sizeX + 1;
            for (int y = 1; y < sizeY - 1; y++)
            {
                for (int x = 1; x < sizeX - 1; x++)
                {
                    int value;

                    value = src[offset - (sizeX + 1)] & 0xFF;
                    value += src[offset - sizeX] & 0xFF;
                    value += src[offset - (sizeX - 1)] & 0xFF;
                    value += src[offset - 1] & 0xFF;
                    value += src[offset] & 0xFF;
                    value += src[offset + 1] & 0xFF;
                    value += src[offset + (sizeX - 1)] & 0xFF;
                    value += src[offset + sizeX] & 0xFF;
                    value += src[offset + (sizeX + 1)] & 0xFF;
                    value /= 9;

                    dst[offset] = (byte) value;
                    offset++;
                }

                offset += 2;
            }

            // notify data changed
            image.dataChanged();
        }

        /**
         * Per pixel access, fixed data type and optimized code
         */
        private void doMethod5(IcyBufferedImage image, int component)
        {
            final byte[] src = image.getDataCopyXYAsByte(component);
            final byte[] dst = image.getDataXYAsByte(component);

            final int sizeX = image.getSizeX();
            final int sizeY = image.getSizeY();

            int offset = sizeX + 1;
            for (int y = 1; y < sizeY - 1; y++)
            {
                int p11, p12;
                int p21, p22;
                int p31, p32;

                p11 = src[offset - (sizeX + 1)] & 0xFF;
                p12 = src[offset - sizeX] & 0xFF;
                p21 = src[offset - 1] & 0xFF;
                p22 = src[offset] & 0xFF;
                p31 = src[offset + (sizeX - 1)] & 0xFF;
                p32 = src[offset + sizeX] & 0xFF;

                for (int x = 1; x < sizeX - 1; x++)
                {
                    final int p13 = src[offset - (sizeX - 1)] & 0xFF;
                    final int p23 = src[offset + 1] & 0xFF;
                    final int p33 = src[offset + (sizeX + 1)] & 0xFF;

                    dst[offset++] = (byte) ((p11 + p12 + p13 + p21 + p23 + p31 + p32 + p33) >> 3);

                    p11 = p12;
                    p21 = p22;
                    p31 = p32;
                    p12 = p13;
                    p22 = p23;
                    p32 = p33;
                }

                offset += 2;
            }

            // notify data changed
            image.dataChanged();
        }
    }

    private static final String METHOD1 = "method1";
    private static final String METHOD2 = "method2";
    private static final String METHOD3 = "method3";
    private static final String METHOD4 = "method4";
    private static final String METHOD5 = "method5";

    ButtonGroup group;
    JLabel label;
    IcyBufferedImage imgIn;
    boolean end;
    boolean paused;

    /*
     * (non-Javadoc)
     * 
     * @see icy.plugin.interface_.PluginImageAnalysis#compute()
     */
    @Override
    public void compute()
    {
        final TitledFrame frame = new TitledFrame("Smooth Performance Test", true, true);

        frame.addFrameListener(new IcyFrameAdapter()
        {
            @Override
            public void icyFrameClosed(IcyFrameEvent e)
            {
                end = true;
            }
        });

        group = new ButtonGroup();
        label = new JLabel(" ");
        ComponentUtil.setFixedWidth(label, 400);
        imgIn = null;
        end = false;
        paused = false;

        final JRadioButton method1Radio = new JRadioButton("Per pixel access, generic data type");
        final JRadioButton method2Radio = new JRadioButton("Per pixel access, fixed data type");
        final JRadioButton method3Radio = new JRadioButton("Per band access, generic data type");
        final JRadioButton method4Radio = new JRadioButton("Per band access, fixed data type");
        final JRadioButton method5Radio = new JRadioButton("Per band access, fix data type, optimized code");

        method1Radio.setActionCommand(METHOD1);
        method2Radio.setActionCommand(METHOD2);
        method3Radio.setActionCommand(METHOD3);
        method4Radio.setActionCommand(METHOD4);
        method5Radio.setActionCommand(METHOD5);

        group.add(method1Radio);
        group.add(method2Radio);
        group.add(method3Radio);
        group.add(method4Radio);
        group.add(method5Radio);

        group.setSelected(method1Radio.getModel(), true);

        final JButton startButton = new JButton("START");
        startButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                execute();
            }
        });

        final JButton stopButton = new JButton("STOP");
        stopButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                paused = true;
            }
        });

        final JPanel panel = frame.getMainPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        panel.add(GuiUtil.createCenteredBoldLabel("Choose a method to see how it performs"));
        panel.add(Box.createVerticalStrut(4));
        panel.add(GuiUtil.createLineBoxPanel(method1Radio, Box.createHorizontalGlue()));
        panel.add(GuiUtil.createLineBoxPanel(method2Radio, Box.createHorizontalGlue()));
        panel.add(GuiUtil.createLineBoxPanel(method3Radio, Box.createHorizontalGlue()));
        panel.add(GuiUtil.createLineBoxPanel(method4Radio, Box.createHorizontalGlue()));
        panel.add(GuiUtil.createLineBoxPanel(method5Radio, Box.createHorizontalGlue()));
        panel.add(Box.createVerticalStrut(4));
        panel.add(GuiUtil.createLineBoxPanel(startButton, stopButton, Box.createHorizontalStrut(8), label,
                Box.createHorizontalGlue()));

        frame.pack();
        addIcyFrame(frame);
        frame.setVisible(true);
        frame.center();
        frame.requestFocus();

        // start thread for processing
        new Thread(new Processor()).start();
    }

    void execute()
    {
        final IcyBufferedImage image = getFocusedImage();

        if (image == null)
            label.setText("No sequence selected");
        else
        {
            imgIn = image.getCopy();
            addSequence(new Sequence(imgIn));
        }

        paused = false;
    }
}
