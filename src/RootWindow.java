package cubesat.groundStation;

// standard libraries
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.EtchedBorder;

// external
import org.apache.log4j.Logger;

public class RootWindow extends javax.swing.JFrame implements ActionListener {
    static Logger guilog = Logger.getLogger(RootWindow.class);
    
    protected static final int preferredWidth=600;
    protected static final int preferredHeight=400;
    protected static final int maxWidth=5000;
    protected static final int maxHeight=5000;
    // this is to enforce the singleton...ness.
    // this means that there should never be more than one of these frames. Enforce it.
    protected static RootWindow onlyInterface=null;

    JMenuBar mainMenu =null;
    JTextArea inDisplayText=null;
    JLabel statusLabel=null;
    GridBagConstraints constr=null;
    
    // back-end handles
    protected SerialNode sourceNode=null;
    protected JMenu serialMenu = null;
    protected JMenuItem sourceStatusItem = null;
    protected JMenuItem sourceConnectItem = null;
    protected DisplayNode dispNode=null;
    protected FileNode logNode=null;
    protected JMenuItem logStatusItem = null;
    protected JMenuItem logConnectItem = null;

    private RootWindow(String Title){
	super(Title);
	setSize(preferredWidth,preferredHeight);
	setMaximumSize(new Dimension(maxWidth, maxHeight));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	getContentPane().setLayout(new GridBagLayout());
	constr = new GridBagConstraints();
    }

    public void actionPerformed(ActionEvent ae){
	String cmd = ae.getActionCommand();
	String[] params = ae.getActionCommand().split("[.]");

	guilog.trace("  .. Event receive@RootWindow= "+ae.getActionCommand());
	guilog.info("  .. Event receive@ConsoleInterface= "+ae.getActionCommand());

	if(params[0].equalsIgnoreCase(sourceNode.getNodeTypeName())){
	    if(params[1].equalsIgnoreCase("update")){
		if(params[2].equalsIgnoreCase("label")){
		    updateSourceLabels();
		}
		if(params[2].equalsIgnoreCase("list")){
		    createSerialMenu();
		}
	    }
	}else if(params[0].equalsIgnoreCase(dispNode.getNodeTypeName())){
	    if(params[1].equalsIgnoreCase("update")){
		//updateFrameDisplay(ByteFrame updateSource);
		updateFrameDisplay(dispNode.getActiveFrame());
	    }
	}else if(params[0].equalsIgnoreCase(logNode.getNodeTypeName())){
	    if(params[1].equalsIgnoreCase("update")){
		// no-op
		//updateTelemLogDisplays();
	    }
	}else if(cmd.equals("Quit")){
	    System.exit(0);
	}else{
	    guilog.warn("!! unrecognized action requested='"+cmd+"'");
	}
    }

    private JMenu createAboutMenu(){
	JMenu aboutMenu=new JMenu("About");  // "aboot, eh?"
	return aboutMenu;
    }
    
    private JMenuBar createMenuBar(){
	mainMenu = new JMenuBar();
	JMenu programMenu = createProgramMenu();
	JMenu portMenu = createSerialMenu();
	JMenu storeMenu = createLogMenu();
	JMenu aboutMenu = createAboutMenu();
	mainMenu.add(programMenu);
	mainMenu.add(portMenu);
	mainMenu.add(storeMenu);
	mainMenu.add(aboutMenu);
	return mainMenu;
    }
	
    private JMenu createSerialMenu(){
	// convert to a tool bar....
	if(null==serialMenu){
	    serialMenu = new JMenu("Serial");
	}else{
	    serialMenu.removeAll();
	}

	if(null==sourceStatusItem){
	    sourceStatusItem = new JMenuItem("<unknown status>");
	}
	serialMenu.add(sourceStatusItem);

	if(null==sourceConnectItem){
	    sourceConnectItem = new JMenuItem("<unk>");
	    sourceConnectItem.addActionListener(sourceNode);
	    sourceConnectItem.setActionCommand("uninitialized command. :(");
	}
	serialMenu.add(sourceConnectItem);

	serialMenu.add(new JSeparator());

	JMenuItem refreshItem = new JMenuItem("Refresh Port List");
	refreshItem.addActionListener(this);
	refreshItem.setActionCommand(sourceNode.getNodeTypeName()+".update.list");
	serialMenu.add(refreshItem);

	Vector<String> portNameList=SerialNode.getPortNameList();
	for(String name: portNameList){
	    JMenuItem curPort = new JMenuItem(name);
	    curPort.addActionListener(sourceNode);
	    curPort.setActionCommand("setport:"+name);
	    serialMenu.add(curPort);
	}

	return serialMenu;
    }

    private JMenu createProgramMenu(){
	// convert to a tool bar....
	JMenu programMenu = new JMenu("Program");
	
	JMenuItem quitItem = new JMenuItem("Quit");
	quitItem.setActionCommand("Quit");
	quitItem.addActionListener(this);
	programMenu.add(quitItem);
	return programMenu;
    }


    private JPanel createStatusBar(){
	JPanel statusBar= new JPanel();
	statusBar.setBorder(BorderFactory.createBevelBorder(EtchedBorder.LOWERED));
	statusLabel = new JLabel("This is my status bar.");
	statusLabel.setMinimumSize(new Dimension(400,32));
	statusBar.add(statusLabel);
	return statusBar;
    }

    private JMenu createLogMenu(){
	JMenu logMenu= new JMenu("Log");

	JMenuItem statusItem = new JMenuItem("<r/w>:<filename>");
	logMenu.add(statusItem);
	JMenuItem openActionItem = new JMenuItem("<unk>");
	openActionItem.setActionCommand("uninitialized command. :(");
	logMenu.add(openActionItem);

	// JPanel fileConfigPanel = new JPanel();
	// fileConfigPanel.setLayout(new FlowLayout());
	// fileConfigPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
	
	// JTextField fileNameField = new JTextField("No File Specified");
	// fileNameField.setColumns(20);
	// fileConfigPanel.add(fileNameField,BorderLayout.PAGE_START);

	// //In initialization code:
	// //Create the radio buttons.
	// JRadioButton readButton = new JRadioButton("Read");
	// readButton.setActionCommand("readmode");
	// readButton.setSelected(true);
	// fileConfigPanel.add(readButton, BorderLayout.PAGE_START);
	
	// JRadioButton writeButton = new JRadioButton("Write");
	// fileConfigPanel.add(writeButton, BorderLayout.PAGE_START);
	// writeButton.setActionCommand("writemode");

	// //Group the radio buttons.
	// // WARNING- Unused at the moment.
	// ButtonGroup modeGroup = new ButtonGroup();
	// modeGroup.add(readButton);
	// modeGroup.add(writeButton);

	// //Register a listener for the radio buttons.
	// //readButton.addActionListener(this);
	// //writeButton.addActionListener(this);
	return logMenu;
    }


	    
    // enforce singleton design pattern
    public static RootWindow getInterface(){
	if(null==onlyInterface){
	    onlyInterface=new RootWindow("Telemetry Monitor");
	}
	return onlyInterface;
    }

    public void initialize(SerialNode _sourceNode, DisplayNode _dispNode, FileNode _logNode){
	if((null==_sourceNode)||(null==_dispNode)||(null==_logNode)){
	    guilog.error("Attempt to populate the gui without populating the model");
	    return;
	}
	sourceNode=_sourceNode;
	dispNode=_dispNode;
	logNode=_logNode;

	constr.fill = GridBagConstraints.FIRST_LINE_START;
	constr.anchor = constr.fill;
	constr.gridx = constr.gridy = 0;
	constr.weightx = constr.weighty = 1.0;
	constr.gridwidth=GridBagConstraints.REMAINDER;
	constr.gridheight=1;
	add(createMenuBar(), constr);
	
	// create hexadecimal display panel
	inDisplayText = new JTextArea();
	inDisplayText.setText("No data received yet.");
	inDisplayText.setEditable(false);
	JPanel inDisplay = new JPanel();
	inDisplay.add(inDisplayText);
	inDisplay.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
	inDisplayText.setBorder(BorderFactory.createTitledBorder(
				    BorderFactory.createLineBorder(Color.black),
				    "Binary Telemetry"));
	//inDisplayText.setMinimumSize(new Dimension(10,1));
	//inDisplayText.setMaximumSize(new Dimension(maxWidth,100));
        inDisplayText.setColumns(40);
	inDisplayText.setRows(6);
	constr.fill = GridBagConstraints.LINE_START;
	constr.anchor = constr.fill;
	constr.gridx = 0;
	constr.gridy = 1;
	constr.gridwidth=GridBagConstraints.REMAINDER;
	constr.gridheight=2;
	constr.weightx=.2;
	constr.weighty=.2;
	add(inDisplay,constr);
	
        //Add the ubiquitous "Hello World" label.
	constr.fill = GridBagConstraints.LAST_LINE_START;
	constr.anchor = constr.fill;
	constr.gridx = 0;
	constr.gridy = 20;
	constr.gridwidth=GridBagConstraints.REMAINDER;
	constr.gridheight=1;
	constr.weightx=constr.weighty = 0.5;
        add(createStatusBar(),constr);


	// do not do this until the firing node AND
	// all gui elements are created.
	sourceNode.addActionListener(this);
	dispNode.addActionListener(this);
	logNode.addActionListener(this);

	updateSourceLabels();
	// update filter displays
	// update log dipslays

        //Display the window.
        pack();
	setSize(preferredWidth,preferredHeight);
        setVisible(true);
    }

    public void setSerialNode(SerialNode _node){ sourceNode=_node;}
    public void setDisplayNode(DisplayNode _node){ dispNode=_node;}
    public void setOutputNode(FileNode _node){ logNode=_node;}

    public void updateFrameDisplay(ByteFrame updateSource){
	//System.out.println(" >> updating frame display elements");
	if(null==updateSource){
	    return;
	}else{
	    // update GUI elements that display the current frame
	    inDisplayText.setText(updateSource.getDataString(24,8));
	    // NYI

	}

    }
    
    public void updateSourceLabels(){
	if (sourceNode.isConnected()){
	    sourceStatusItem.setText("Connected // "+sourceNode.getPortName()+" // "+sourceNode.getBaud());
	    sourceConnectItem.setText("Disconnect");
	    sourceConnectItem.setActionCommand("disconnect");
	}else{
	    sourceStatusItem.setText("<Not Connected> // "+sourceNode.getPortName()+" // "+sourceNode.getBaud());
	    sourceConnectItem.setText("Connect");
	    sourceConnectItem.setActionCommand("connect");
	}

    }
    

}
