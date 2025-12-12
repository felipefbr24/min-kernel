import archivos.EntradaArchivo;
import dto.ResultadoTick;
import es.SolicitudES;
import memoria.BloqueMemoria;
import procesos.BloqueControlProceso;
import visual.EntradaPlan;
import visual.PanelPlan;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class KernelUI extends JFrame {
    private final SimuladorKernel simulator = new SimuladorKernel();
    private final DefaultTableModel processModel = new DefaultTableModel(new Object[]{"PID", "Nombre", "Estado", "Restante", "Memoria", "Archivos"}, 0);
    private final DefaultTableModel fileModel = new DefaultTableModel(new Object[]{"Nombre", "Propietario", "Abierto por"}, 0);
    private final JTextArea memoryArea = new JTextArea();
    private final JTextArea ioArea = new JTextArea();
    private final JTextArea logArea = new JTextArea();
    private final JLabel memorySummary = new JLabel("Memoria");
    private final JProgressBar memoryBar = new JProgressBar(0, 100);
    private final List<EntradaPlan> schedule = new ArrayList<>();
    private final PanelPlan schedulePanel = new PanelPlan(schedule);
    private final Color primario = new Color(0x0B3C5D);
    private final Color acento = new Color(0xEE6352);
    private final Color fondo = new Color(0xF5F7FA);
    private final Color panelBlanco = Color.WHITE;
    private final Color texto = new Color(0x1E1E1E);
    private JTable processTable;
    private final JSpinner quantumSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
    private final JSpinner autoDelaySpinner = new JSpinner(new SpinnerNumberModel(700, 100, 5000, 100));
    private javax.swing.Timer autoTimer;
    private JButton autoButton;
    private final JComboBox<Integer> processSelector = new JComboBox<>();
    private final JComboBox<Integer> fileProcessSelector = new JComboBox<>();
    private final JComboBox<String> deviceSelector = new JComboBox<>(new String[]{"Teclado", "Disco"});

    public KernelUI() {
        setTitle("Mini Kernel - Simulador de SO");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(fondo);
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(12, 12, 12, 12));

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);
        add(buildLogPanel(), BorderLayout.SOUTH);
        refreshUI();
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, primario, getWidth(), getHeight(), primario.darker()));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setOpaque(true);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        JLabel titulo = new JLabel("Simulación de procesos, memoria, archivos y E/S");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(titulo);
        JButton tickButton = crearBoton("Ciclo CPU", acento, Color.WHITE);
        tickButton.addActionListener(e -> tickOnce());
        panel.add(tickButton);
        autoButton = crearBoton("CPU auto", Color.WHITE, primario.darker());
        autoButton.addActionListener(e -> toggleAuto());
        panel.add(autoButton);
        JButton resetButton = crearBoton("Limpiar todo", Color.WHITE, primario.darker());
        resetButton.addActionListener(e -> {
            simulator.reset();
            schedule.clear();
            stopAuto();
            logArea.setText("");
            log("Sistema reiniciado");
            refreshUI();
        });
        panel.add(resetButton);
        quantumSpinner.setValue(simulator.getQuantum());
        JButton setQuantum = crearBoton("Actualizar quantum", Color.WHITE, primario.darker());
        setQuantum.addActionListener(e -> {
            int q = (int) quantumSpinner.getValue();
            simulator.setQuantum(q);
            log("Quantum actualizado a " + q);
        });
        JLabel quantumLabel = new JLabel("Quantum:");
        quantumLabel.setForeground(Color.WHITE);
        panel.add(quantumLabel);
        panel.add(quantumSpinner);
        panel.add(setQuantum);
        JLabel delayLabel = new JLabel("Intervalo auto (ms):");
        delayLabel.setForeground(Color.WHITE);
        panel.add(delayLabel);
        panel.add(autoDelaySpinner);
        return panel;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(fondo);
        tabs.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        tabs.addTab("Procesos", buildProcessPanel());
        tabs.addTab("Memoria", buildMemoryPanel());
        tabs.addTab("Archivos y E/S", buildFilesAndIOPanel());
        return tabs;
    }

    private JPanel buildProcessPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.setBackground(panelBlanco);
        JTextField nameField = new JTextField("Proceso", 10);
        JSpinner burstField = new JSpinner(new SpinnerNumberModel(5, 1, 50, 1));
        JSpinner memField = new JSpinner(new SpinnerNumberModel(32, 4, 256, 4));
        JButton createButton = crearBoton("Crear proceso", acento, Color.WHITE);
        createButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            int burst = (int) burstField.getValue();
            int mem = (int) memField.getValue();
            String result = simulator.createProcess(name, burst, mem);
            log(result);
            refreshUI();
        });
        form.setBorder(new EmptyBorder(5, 5, 5, 5));
        form.add(new JLabel("Nombre:"));
        form.add(nameField);
        form.add(new JLabel("Ráfaga:"));
        form.add(burstField);
        form.add(new JLabel("Memoria:"));
        form.add(memField);
        form.add(createButton);

        JTable table = new JTable(processModel);
        this.processTable = table;
        JScrollPane tableScroll = new JScrollPane(table);
        table.setRowHeight(24);
        table.setGridColor(new Color(230, 230, 230));
        table.setSelectionBackground(new Color(224, 242, 254));
        table.setSelectionForeground(Color.DARK_GRAY);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(0xE8EEF4));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xCED6E0)));
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Integer pid = getSelectedPidFromTable();
                if (pid != null) {
                    processSelector.setSelectedItem(pid);
                    fileProcessSelector.setSelectedItem(pid);
                }
            }
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setBackground(panelBlanco);
        JButton blockIO = crearBoton("Solicitar E/S", primario, Color.WHITE);
        blockIO.addActionListener(e -> {
            Integer pid = getSelectedPid();
            if (pid == null) {
                log("Seleccione un proceso para E/S");
                return;
            }
            String result = simulator.requestIO(pid, (String) deviceSelector.getSelectedItem(), "Interacción manual");
            log(result);
            refreshUI();
        });

        JButton terminate = crearBoton("Terminar proceso", new Color(244, 67, 54), Color.WHITE);
        terminate.addActionListener(e -> {
            Integer pid = getSelectedPid();
            if (pid == null) {
                log("Seleccione un proceso para terminar");
                return;
            }
            String result = simulator.forceTerminate(pid);
            log(result);
            refreshUI();
        });
        actions.add(new JLabel("Proceso:"));
        actions.add(processSelector);
        actions.add(blockIO);
        actions.add(terminate);

        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(panelBlanco);
        south.add(actions, BorderLayout.NORTH);
        schedulePanel.setPreferredSize(new Dimension(200, 140));
        schedulePanel.setBackground(panelBlanco);
        south.add(schedulePanel, BorderLayout.CENTER);
        panel.add(wrapCard(form), BorderLayout.NORTH);
        panel.add(wrapCard(tableScroll), BorderLayout.CENTER);
        panel.add(wrapCard(south), BorderLayout.SOUTH);
        panel.setBackground(fondo);
        return panel;
    }

    private JPanel buildMemoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        memoryArea.setEditable(false);
        memoryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        memoryArea.setBackground(Color.WHITE);
        memoryBar.setStringPainted(true);
        JPanel info = new JPanel(new BorderLayout(5, 5));
        info.setBackground(Color.WHITE);
        info.add(memorySummary, BorderLayout.WEST);
        info.add(memoryBar, BorderLayout.CENTER);
        panel.add(wrapCard(new JScrollPane(memoryArea)), BorderLayout.CENTER);
        panel.add(wrapCard(info), BorderLayout.NORTH);
        panel.setBackground(fondo);
        return panel;
    }

    private JPanel buildFilesAndIOPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filePanel.setBackground(Color.WHITE);
        JTextField fileNameField = new JTextField("archivo.txt", 12);
        JButton createFile = crearBoton("Crear", primario, Color.WHITE);
        createFile.addActionListener(e -> {
            Integer pid = (Integer) fileProcessSelector.getSelectedItem();
            if (pid == null) {
                log("Seleccione un proceso propietario");
                return;
            }
            String result = simulator.createFile(pid, fileNameField.getText().trim());
            log(result);
            refreshUI();
        });
        JButton openFile = crearBoton("Abrir", primario, Color.WHITE);
        openFile.addActionListener(e -> {
            Integer pid = (Integer) fileProcessSelector.getSelectedItem();
            if (pid == null) {
                log("Seleccione un proceso para abrir archivo");
                return;
            }
            String result = simulator.openFile(pid, fileNameField.getText().trim());
            log(result);
            refreshUI();
        });
        JButton closeFile = crearBoton("Cerrar", new Color(244, 67, 54), Color.WHITE);
        closeFile.addActionListener(e -> {
            Integer pid = (Integer) fileProcessSelector.getSelectedItem();
            if (pid == null) {
                log("Seleccione un proceso para cerrar archivo");
                return;
            }
            String result = simulator.closeFile(pid, fileNameField.getText().trim());
            log(result);
            refreshUI();
        });
        filePanel.add(new JLabel("Proceso:"));
        filePanel.add(fileProcessSelector);
        filePanel.add(new JLabel("Archivo:"));
        filePanel.add(fileNameField);
        filePanel.add(createFile);
        filePanel.add(openFile);
        filePanel.add(closeFile);

        JTable fileTable = new JTable(fileModel);
        JScrollPane fileScroll = new JScrollPane(fileTable);

        JPanel ioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ioPanel.setBackground(Color.WHITE);
        JTextField ioDetail = new JTextField("Leer entrada", 12);
        JButton requestIO = crearBoton("Solicitar", primario, Color.WHITE);
        requestIO.addActionListener(e -> {
            Integer pid = (Integer) processSelector.getSelectedItem();
            if (pid == null) {
                log("Seleccione un proceso para E/S");
                return;
            }
            String result = simulator.requestIO(pid, (String) deviceSelector.getSelectedItem(), ioDetail.getText().trim());
            log(result);
            refreshUI();
        });
        JButton completeIO = crearBoton("Completar siguiente interrupción", acento, Color.WHITE);
        completeIO.addActionListener(e -> {
            String result = simulator.completeIO();
            log(result);
            refreshUI();
        });
        ioPanel.add(new JLabel("Proceso:"));
        ioPanel.add(processSelector);
        ioPanel.add(new JLabel("Dispositivo:"));
        ioPanel.add(deviceSelector);
        ioPanel.add(new JLabel("Detalle:"));
        ioPanel.add(ioDetail);
        ioPanel.add(requestIO);
        ioPanel.add(completeIO);

        ioArea.setEditable(false);
        ioArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        ioArea.setBackground(Color.WHITE);

        JPanel center = new JPanel(new GridLayout(1, 2, 10, 10));
        center.setBackground(fondo);
        center.add(wrapCard(fileScroll));
        center.add(wrapCard(new JScrollPane(ioArea)));

        panel.add(wrapCard(filePanel), BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        panel.add(wrapCard(ioPanel), BorderLayout.SOUTH);
        panel.setBackground(fondo);
        return panel;
    }

    private JScrollPane buildLogPanel() {
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setBackground(Color.WHITE);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setPreferredSize(new Dimension(200, 120));
        scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(0xE0E6ED)),
                new EmptyBorder(6, 6, 6, 6)
        ));
        return scroll;
    }

    private void refreshUI() {
        refreshProcessTable();
        refreshMemory();
        refreshFiles();
        refreshSelectors();
        refreshIOQueue();
    }

    private void refreshProcessTable() {
        processModel.setRowCount(0);
        for (BloqueControlProceso pcb : simulator.getProcesses()) {
            processModel.addRow(new Object[]{
                    pcb.pid,
                    pcb.name,
                    pcb.state,
                    pcb.remainingTime,
                    pcb.memoryNeeded,
                    pcb.openFiles.size()
            });
        }
    }

    private void refreshMemory() {
        int total = simulator.getTotalMemory();
        int used = simulator.getUsedMemory();
        int free = simulator.getFreeMemory();
        int percent = total == 0 ? 0 : (int) Math.round((used * 100.0) / total);
        memorySummary.setText("Total: " + total + " KB | Usada: " + used + " KB | Libre: " + free + " KB");
        memoryBar.setValue(percent);
        memoryBar.setString(percent + "%");
        StringBuilder builder = new StringBuilder();
        for (BloqueMemoria block : simulator.getMemoryBlocks()) {
            builder.append(String.format("%03d-%03d | %4d | %s%n",
                    block.start,
                    block.start + block.size - 1,
                    block.size,
                    block.isFree() ? "Libre" : "PID " + block.pid));
        }
        memoryArea.setText(builder.toString());
    }

    private void refreshFiles() {
        fileModel.setRowCount(0);
        for (EntradaArchivo file : simulator.getFiles()) {
            fileModel.addRow(new Object[]{file.name, file.ownerPid, file.openCount});
        }
    }

    private void refreshIOQueue() {
        StringBuilder builder = new StringBuilder();
        builder.append("Cola de solicitudes de E/S:\n");
        for (SolicitudES req : simulator.getIoQueue()) {
            builder.append(String.format("PID %d -> %s (%s)%n", req.pid, req.device, req.detail));
        }
        ioArea.setText(builder.toString());
    }

    private void refreshSelectors() {
        List<Integer> pids = simulator.getSelectablePids();
        processSelector.removeAllItems();
        fileProcessSelector.removeAllItems();
        for (Integer pid : pids) {
            processSelector.addItem(pid);
            fileProcessSelector.addItem(pid);
        }
    }

    private void log(String message) {
        String time = LocalTime.now().withNano(0).toString();
        logArea.append("[" + time + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void tickOnce() {
        ResultadoTick result = simulator.tick();
        log(result.message);
        schedule.add(new EntradaPlan(result.pid, result.name));
        if (schedule.size() > 60) {
            schedule.remove(0);
        }
        schedulePanel.repaint();
        refreshUI();
    }

    private void toggleAuto() {
        if (autoTimer != null && autoTimer.isRunning()) {
            stopAuto();
            return;
        }
        int delay = (int) autoDelaySpinner.getValue();
        autoTimer = new javax.swing.Timer(delay, e -> tickOnce());
        autoTimer.start();
        autoButton.setText("Detener auto");
    }

    private void stopAuto() {
        if (autoTimer != null) {
            autoTimer.stop();
            autoTimer = null;
        }
        if (autoButton != null) {
            autoButton.setText("CPU auto");
        }
    }

    private Integer getSelectedPid() {
        Integer comboPid = (Integer) processSelector.getSelectedItem();
        if (comboPid != null) {
            return comboPid;
        }
        return getSelectedPidFromTable();
    }

    private Integer getSelectedPidFromTable() {
        if (processTable == null) {
            return null;
        }
        int row = processTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        Object value = processModel.getValueAt(row, 0);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private JButton crearBoton(String texto, Color fondoBoton, Color colorTexto) {
        JButton b = new JButton(texto);
        b.setFocusPainted(false);
        b.setBackground(fondoBoton);
        b.setForeground(colorTexto);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(0xD9E2EC)),
                new EmptyBorder(8, 12, 8, 12)
        ));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JPanel wrapCard(JComponent contenido) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(panelBlanco);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(0xE0E6ED)),
                new EmptyBorder(8, 8, 8, 8)
        ));
        card.add(contenido, BorderLayout.CENTER);
        return card;
    }
}
