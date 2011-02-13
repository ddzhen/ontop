/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.obda.reformulation.protege4.configpanel;

import inf.unibz.it.obda.api.controller.APIController;
import inf.unibz.it.obda.api.controller.DatasourcesControllerListener;
import inf.unibz.it.obda.domain.DataSource;
import inf.unibz.it.obda.rdbmsgav.domain.RDBMSsourceParameterConstants;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.obda.owlrefplatform.core.ABoxToDBDumper;
import org.semanticweb.owl.model.OWLOntology;

/*
 * SelectDB.java
 *
 * Created on 27-set-2010, 9.41.00
 */

/**
 *
 * @author obda
 */
public class SelectDB extends javax.swing.JDialog implements DatasourcesControllerListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1787461016329735072L;
	private APIController apic = null;
	private SetParametersDialog dialog = null;
	private ABoxToDBDumper dumper = null;
	private Set<OWLOntology> ontologies = null;
	
    /** Creates new form SelectDB */
    public SelectDB(java.awt.Frame parent, boolean modal, APIController apic, ABoxToDBDumper dumper, Set<OWLOntology> ontologies) {
        super(parent, modal);
        this.apic = apic;
        this.dumper = dumper;
        this.ontologies = ontologies;
        initComponents();
        addListener();
        addItemsToCombo();
        apic.getDatasourcesController().addDatasourceControllerListener(this);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        jButton3 = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jComboBox1 = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Select Data Source");
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jButton3.setText("OK");
        jButton3.setMaximumSize(new java.awt.Dimension(50, 25));
        jButton3.setMinimumSize(new java.awt.Dimension(50, 25));
        jButton3.setPreferredSize(new java.awt.Dimension(50, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jButton3, gridBagConstraints);

        jButton1.setText("New...");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(jButton1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 15, 0);
        jPanel1.add(jComboBox1, gridBagConstraints);

        jLabel1.setText("Select the data source where to dump the ABox:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 1, 15, 1);
        jPanel1.add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 15);
        getContentPane().add(jPanel1, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jLabel2, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(jLabel3, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void addListener(){
    	jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	jButton3ActionPerformed(evt);
            }
        });
    }
    
    private void addItemsToCombo(){
    	
    	HashMap<URI, DataSource> map = apic.getDatasourcesController().getAllSources();
    	Iterator<DataSource> it= map.values().iterator();
    	while(it.hasNext()){
    		DataSource ds = it.next();
    		String usage = ds.getParameter(RDBMSsourceParameterConstants.USE_DATASOURCE_FOR_ABOXDUMP);
    		if(usage != null && usage.equals("true")){
    			jComboBox1.addItem(ds.getSourceID().toString());
    		}
    	}
    	
    }
    
    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {
    	this.setVisible(false);
    	String name = jComboBox1.getSelectedItem().toString();  
    	try {
			dumper.materialize(ontologies, URI.create(name), false);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Error during the dumping. Please check the logfile for more information", "FAILURE", JOptionPane.ERROR);
			e.printStackTrace();
		}
    }
    
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
       
    	 dialog = new SetParametersDialog(new JFrame(), false,apic);
         dialog.setLocationRelativeTo(this);
         dialog.setVisible(true);
    	
    }//GEN-LAST:event_jButton1ActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton3;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables

	@Override
	public void alldatasourcesDeleted() {
		if(isVisible()){
			addItemsToCombo();
		}
		
	}

	@Override
	public void currentDatasourceChange(DataSource previousdatasource,
			DataSource currentsource) {
		if(isVisible()){
			addItemsToCombo();
		}
		
	}

	@Override
	public void datasourcParametersUpdated() {
		if(isVisible()){
			addItemsToCombo();
		}
		
	}

	@Override
	public void datasourceAdded(DataSource source) {
		if(isVisible()){
			addItemsToCombo();
		}
		
	}

	@Override
	public void datasourceDeleted(DataSource source) {
		if(isVisible()){
			addItemsToCombo();
		}
		
	}

	@Override
	public void datasourceUpdated(String oldname, DataSource currendata) {
		if(isVisible()){
			addItemsToCombo();
		}
		
	}
	
	public void dispose(){
		apic.getDatasourcesController().removeDatasourceControllerListener(this);
		super.dispose();
	}
}
