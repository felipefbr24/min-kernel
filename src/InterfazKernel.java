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

public class InterfazKernel extends JFrame {
    private final SimuladorKernel simulador = new SimuladorKernel();
    private final DefaultTableModel modeloProcesos = new DefaultTableModel(new Object[]{"PID", "Nombre", "Estado", "Restante", "Memoria", "Archivos"}, 0);
    private final DefaultTableModel modeloArchivos = new DefaultTableModel(new Object[]{"Nombre", "Propietario", "Abierto por"}, 0);
    private final JTextArea areaMemoria = new JTextArea();
    private final JTextArea areaES = new JTextArea();
    private final JTextArea areaLog = new JTextArea();
    private final JLabel resumenMemoria = new JLabel("Memoria");
    private final JProgressBar barraMemoria = new JProgressBar(0, 100);
    private final List<EntradaPlan> planificacion = new ArrayList<>();
    private final PanelPlan panelPlanificacion = new PanelPlan(planificacion);
    private final Color primario = new Color(0x0B3C5D);
    private final Color acento = new Color(0xEE6352);
    private final Color fondo = new Color(0xF5F7FA);
    private final Color panelBlanco = Color.WHITE;
    private final Color texto = new Color(0x1E1E1E);
    private JTable tablaProcesos;
    private final JSpinner spinnerCuanto = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
    private final JSpinner spinnerIntervaloAuto = new JSpinner(new SpinnerNumberModel(700, 100, 5000, 100));
    private javax.swing.Timer temporizadorAuto;
    private JButton botonAuto;
    private final JComboBox<Integer> selectorProcesos = new JComboBox<>();
    private final JComboBox<Integer> selectorProcesosArchivo = new JComboBox<>();
    private final JComboBox<String> selectorDispositivo = new JComboBox<>(new String[]{"Teclado", "Disco"});

    public InterfazKernel() {
        setTitle("Mini Kernel - Simulador de SO");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(fondo);
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(12, 12, 12, 12));

        add(construirPanelSuperior(), BorderLayout.NORTH);
        add(construirPestanas(), BorderLayout.CENTER);
        add(construirPanelLog(), BorderLayout.SOUTH);
        actualizarUI();
    }

    private JPanel construirPanelSuperior() {
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
        tickButton.addActionListener(e -> ejecutarCiclo());
        panel.add(tickButton);
        botonAuto = crearBoton("CPU auto", Color.WHITE, primario.darker());
        botonAuto.addActionListener(e -> alternarAuto());
        panel.add(botonAuto);
        JButton resetButton = crearBoton("Limpiar todo", Color.WHITE, primario.darker());
        resetButton.addActionListener(e -> {
            simulador.reiniciar();
            planificacion.clear();
            detenerAuto();
            areaLog.setText("");
            registrar("Sistema reiniciado");
            actualizarUI();
        });
        panel.add(resetButton);
        spinnerCuanto.setValue(simulador.obtenerCuanto());
        JButton setQuantum = crearBoton("Actualizar quantum", Color.WHITE, primario.darker());
        setQuantum.addActionListener(e -> {
            int q = (int) spinnerCuanto.getValue();
            simulador.configurarCuanto(q);
            registrar("Quantum actualizado a " + q);
        });
        JLabel quantumLabel = new JLabel("Quantum:");
        quantumLabel.setForeground(Color.WHITE);
        panel.add(quantumLabel);
        panel.add(spinnerCuanto);
        panel.add(setQuantum);
        JLabel delayLabel = new JLabel("Intervalo auto (ms):");
        delayLabel.setForeground(Color.WHITE);
        panel.add(delayLabel);
        panel.add(spinnerIntervaloAuto);
        return panel;
    }

    private JTabbedPane construirPestanas() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(fondo);
        tabs.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        tabs.addTab("Procesos", construirPanelProcesos());
        tabs.addTab("Memoria", construirPanelMemoria());
        tabs.addTab("Archivos y E/S", construirPanelArchivosES());
        return tabs;
    }

    private JPanel construirPanelProcesos() {
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
            String result = simulador.crearProceso(name, burst, mem);
            registrar(result);
            actualizarUI();
        });
        form.setBorder(new EmptyBorder(5, 5, 5, 5));
        form.add(new JLabel("Nombre:"));
        form.add(nameField);
        form.add(new JLabel("Ráfaga:"));
        form.add(burstField);
        form.add(new JLabel("Memoria:"));
        form.add(memField);
        form.add(createButton);

        JTable table = new JTable(modeloProcesos);
        this.tablaProcesos = table;
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
                Integer pid = obtenerPidSeleccionadoTabla();
                if (pid != null) {
                    selectorProcesos.setSelectedItem(pid);
                    selectorProcesosArchivo.setSelectedItem(pid);
                }
            }
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setBackground(panelBlanco);
        JButton blockIO = crearBoton("Solicitar E/S", primario, Color.WHITE);
        blockIO.addActionListener(e -> {
            Integer pid = obtenerPidSeleccionado();
            if (pid == null) {
                registrar("Seleccione un proceso para E/S");
                return;
            }
            String result = simulador.solicitarES(pid, (String) selectorDispositivo.getSelectedItem(), "Interacción manual");
            registrar(result);
            actualizarUI();
        });

        JButton terminate = crearBoton("Terminar proceso", new Color(244, 67, 54), Color.WHITE);
        terminate.addActionListener(e -> {
            Integer pid = obtenerPidSeleccionado();
            if (pid == null) {
                registrar("Seleccione un proceso para terminar");
                return;
            }
            String result = simulador.forzarTerminacion(pid);
            registrar(result);
            actualizarUI();
        });
        actions.add(new JLabel("Proceso:"));
        actions.add(selectorProcesos);
        actions.add(blockIO);
        actions.add(terminate);

        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(panelBlanco);
        south.add(actions, BorderLayout.NORTH);
        panelPlanificacion.setPreferredSize(new Dimension(200, 140));
        panelPlanificacion.setBackground(panelBlanco);
        south.add(panelPlanificacion, BorderLayout.CENTER);
        panel.add(wrapCard(form), BorderLayout.NORTH);
        panel.add(wrapCard(tableScroll), BorderLayout.CENTER);
        panel.add(wrapCard(south), BorderLayout.SOUTH);
        panel.setBackground(fondo);
        return panel;
    }

    private JPanel construirPanelMemoria() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        areaMemoria.setEditable(false);
        areaMemoria.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        areaMemoria.setBackground(Color.WHITE);
        barraMemoria.setStringPainted(true);
        JPanel info = new JPanel(new BorderLayout(5, 5));
        info.setBackground(Color.WHITE);
        info.add(resumenMemoria, BorderLayout.WEST);
        info.add(barraMemoria, BorderLayout.CENTER);
        panel.add(wrapCard(new JScrollPane(areaMemoria)), BorderLayout.CENTER);
        panel.add(wrapCard(info), BorderLayout.NORTH);
        panel.setBackground(fondo);
        return panel;
    }

    private JPanel construirPanelArchivosES() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filePanel.setBackground(Color.WHITE);
        JTextField fileNameField = new JTextField("archivo.txt", 12);
        JButton createFile = crearBoton("Crear", primario, Color.WHITE);
        createFile.addActionListener(e -> {
            Integer pid = (Integer) selectorProcesosArchivo.getSelectedItem();
            if (pid == null) {
                registrar("Seleccione un proceso propietario");
                return;
            }
            String result = simulador.crearArchivo(pid, fileNameField.getText().trim());
            registrar(result);
            actualizarUI();
        });
        JButton openFile = crearBoton("Abrir", primario, Color.WHITE);
        openFile.addActionListener(e -> {
            Integer pid = (Integer) selectorProcesosArchivo.getSelectedItem();
            if (pid == null) {
                registrar("Seleccione un proceso para abrir archivo");
                return;
            }
            String result = simulador.abrirArchivo(pid, fileNameField.getText().trim());
            registrar(result);
            actualizarUI();
        });
        JButton closeFile = crearBoton("Cerrar", new Color(244, 67, 54), Color.WHITE);
        closeFile.addActionListener(e -> {
            Integer pid = (Integer) selectorProcesosArchivo.getSelectedItem();
            if (pid == null) {
                registrar("Seleccione un proceso para cerrar archivo");
                return;
            }
            String result = simulador.cerrarArchivo(pid, fileNameField.getText().trim());
            registrar(result);
            actualizarUI();
        });
        filePanel.add(new JLabel("Proceso:"));
        filePanel.add(selectorProcesosArchivo);
        filePanel.add(new JLabel("Archivo:"));
        filePanel.add(fileNameField);
        filePanel.add(createFile);
        filePanel.add(openFile);
        filePanel.add(closeFile);

        JTable fileTable = new JTable(modeloArchivos);
        JScrollPane fileScroll = new JScrollPane(fileTable);

        JPanel ioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ioPanel.setBackground(Color.WHITE);
        JTextField ioDetail = new JTextField("Leer entrada", 12);
        JButton requestIO = crearBoton("Solicitar", primario, Color.WHITE);
        requestIO.addActionListener(e -> {
            Integer pid = (Integer) selectorProcesos.getSelectedItem();
            if (pid == null) {
                registrar("Seleccione un proceso para E/S");
                return;
            }
            String result = simulador.solicitarES(pid, (String) selectorDispositivo.getSelectedItem(), ioDetail.getText().trim());
            registrar(result);
            actualizarUI();
        });
        JButton completeIO = crearBoton("Completar siguiente interrupción", acento, Color.WHITE);
        completeIO.addActionListener(e -> {
            String result = simulador.completarES();
            registrar(result);
            actualizarUI();
        });
        ioPanel.add(new JLabel("Proceso:"));
        ioPanel.add(selectorProcesos);
        ioPanel.add(new JLabel("Dispositivo:"));
        ioPanel.add(selectorDispositivo);
        ioPanel.add(new JLabel("Detalle:"));
        ioPanel.add(ioDetail);
        ioPanel.add(requestIO);
        ioPanel.add(completeIO);

        areaES.setEditable(false);
        areaES.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        areaES.setBackground(Color.WHITE);

        JPanel center = new JPanel(new GridLayout(1, 2, 10, 10));
        center.setBackground(fondo);
        center.add(wrapCard(fileScroll));
        center.add(wrapCard(new JScrollPane(areaES)));

        panel.add(wrapCard(filePanel), BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        panel.add(wrapCard(ioPanel), BorderLayout.SOUTH);
        panel.setBackground(fondo);
        return panel;
    }

    private JScrollPane construirPanelLog() {
        areaLog.setEditable(false);
        areaLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        areaLog.setBackground(Color.WHITE);
        areaLog.setLineWrap(true);
        areaLog.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(areaLog);
        scroll.setPreferredSize(new Dimension(200, 120));
        scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(0xE0E6ED)),
                new EmptyBorder(6, 6, 6, 6)
        ));
        return scroll;
    }

    private void actualizarUI() {
        actualizarTablaProcesos();
        actualizarMemoria();
        actualizarArchivos();
        actualizarSelectores();
        actualizarColaES();
    }

    private void actualizarTablaProcesos() {
        modeloProcesos.setRowCount(0);
        for (BloqueControlProceso pcb : simulador.obtenerProcesos()) {
            modeloProcesos.addRow(new Object[]{
                    pcb.pid,
                    pcb.nombre,
                    pcb.estado,
                    pcb.tiempoRestante,
                    pcb.memoriaNecesaria,
                    pcb.archivosAbiertos.size()
            });
        }
    }

    private void actualizarMemoria() {
        int total = simulador.obtenerMemoriaTotal();
        int usada = simulador.obtenerMemoriaUsada();
        int libre = simulador.obtenerMemoriaLibre();
        int porcentaje = total == 0 ? 0 : (int) Math.round((usada * 100.0) / total);
        resumenMemoria.setText("Total: " + total + " KB | Usada: " + usada + " KB | Libre: " + libre + " KB");
        barraMemoria.setValue(porcentaje);
        barraMemoria.setString(porcentaje + "%");
        StringBuilder builder = new StringBuilder();
        for (BloqueMemoria bloque : simulador.obtenerBloquesMemoria()) {
            builder.append(String.format("%03d-%03d | %4d | %s%n",
                    bloque.inicio,
                    bloque.inicio + bloque.tamano - 1,
                    bloque.tamano,
                    bloque.estaLibre() ? "Libre" : "PID " + bloque.pid));
        }
        areaMemoria.setText(builder.toString());
    }

    private void actualizarArchivos() {
        modeloArchivos.setRowCount(0);
        for (EntradaArchivo archivo : simulador.obtenerArchivos()) {
            modeloArchivos.addRow(new Object[]{archivo.nombre, archivo.pidPropietario, archivo.abiertoPorPid});
        }
    }

    private void actualizarColaES() {
        StringBuilder builder = new StringBuilder();
        builder.append("Cola de solicitudes de E/S:\n");
        for (SolicitudES solicitud : simulador.obtenerColaES()) {
            builder.append(String.format("PID %d -> %s (%s)%n", solicitud.pid, solicitud.dispositivo, solicitud.detalle));
        }
        areaES.setText(builder.toString());
    }

    private void actualizarSelectores() {
        List<Integer> pids = simulador.obtenerPidsSeleccionables();
        selectorProcesos.removeAllItems();
        selectorProcesosArchivo.removeAllItems();
        for (Integer pid : pids) {
            selectorProcesos.addItem(pid);
            selectorProcesosArchivo.addItem(pid);
        }
    }

    private void registrar(String mensaje) {
        String hora = LocalTime.now().withNano(0).toString();
        areaLog.append("[" + hora + "] " + mensaje + "\n");
        areaLog.setCaretPosition(areaLog.getDocument().getLength());
    }

    private void ejecutarCiclo() {
        ResultadoTick resultado = simulador.avanzarTick();
        registrar(resultado.mensaje);
        planificacion.add(new EntradaPlan(resultado.pid, resultado.nombre));
        if (planificacion.size() > 60) {
            planificacion.remove(0);
        }
        panelPlanificacion.repaint();
        actualizarUI();
    }

    private void alternarAuto() {
        if (temporizadorAuto != null && temporizadorAuto.isRunning()) {
            detenerAuto();
            return;
        }
        int delay = (int) spinnerIntervaloAuto.getValue();
        temporizadorAuto = new javax.swing.Timer(delay, e -> ejecutarCiclo());
        temporizadorAuto.start();
        botonAuto.setText("Detener auto");
    }

    private void detenerAuto() {
        if (temporizadorAuto != null) {
            temporizadorAuto.stop();
            temporizadorAuto = null;
        }
        if (botonAuto != null) {
            botonAuto.setText("CPU auto");
        }
    }

    private Integer obtenerPidSeleccionado() {
        Integer comboPid = (Integer) selectorProcesos.getSelectedItem();
        if (comboPid != null) {
            return comboPid;
        }
        return obtenerPidSeleccionadoTabla();
    }

    private Integer obtenerPidSeleccionadoTabla() {
        if (tablaProcesos == null) {
            return null;
        }
        int row = tablaProcesos.getSelectedRow();
        if (row < 0) {
            return null;
        }
        Object value = modeloProcesos.getValueAt(row, 0);
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
