/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUIPack;

import LibPack.DataLib;

import java.awt.FileDialog;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;

public class ManageDocumentFrm extends javax.swing.JFrame {

    /**
     * Creates new form CommunicateFrm
     */
    String path;
    String name;
    String fileHash;
    boolean running = false;
    DataLib lib = null;
    DataLib libforDuplicateCheck = null;
    Timer t;
    ArrayList<DataLib> list = new ArrayList<DataLib>();
    MyUploadTask Utask;
    boolean isDuplicateFile = false;

    public ManageDocumentFrm() {
        initComponents();
        setLocationRelativeTo(null);

        Utask = new MyUploadTask(this);
        t = new Timer();
        t.schedule(Utask, 100, 10);
    }

    String getSHA(String str) {
        try {
            File f = new File(str);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            FileInputStream fis = new FileInputStream(f);
            byte[] datArr = new byte[fis.available()];
            fis.read(datArr);
            fis.close();
            md.update(datArr, 0, datArr.length);
            byte[] sha1hash = md.digest();
            StringBuilder sb = new StringBuilder();
            int halfByte = 0;
            for (int i = 0; i < sha1hash.length; i++) {
                halfByte = (sha1hash[i] >> 4) & 0xf;
                if (halfByte < 10) {
                    sb.append((char) (halfByte + 48));
                } else {
                    sb.append((char) (halfByte + 55));
                }
                halfByte = sha1hash[i] & 0xf;
                if (halfByte < 10) {
                    sb.append((char) (halfByte + 48));
                } else {
                    sb.append((char) (halfByte + 55));
                }
            }
            //  System.out.println("Hash: " + sb.toString());
            return sb.toString();

        } catch (Exception e) {
            // TODO: handle exception
            System.out.println("Error In Genrating Signature" + e);
        }
        return "";
    }

    boolean call_Servlet() {
        boolean respFromServer = false;
        try {

            String urlstr = "http://" + Settings.serverIP + ":8084/PortalServer/FileUploadServlets";
            URL url = new URL(urlstr);
            URLConnection connection = url.openConnection();

            connection.setDoOutput(true);
            connection.setDoInput(true);

            // don't use a cached version of URL connection
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // specify the content type that binary data is sent
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
            // send and serialize the object

            out.writeObject(lib);
            out.close();

            // define a new ObjectInputStream on the input stream
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
            // receive and deserialize the object, note the cast
            respFromServer = (boolean) in.readObject();

            in.close();

        } catch (Exception e) {

            System.out.println("Error: " + e);
            e.printStackTrace();
        }
        return respFromServer;
    }

    void call_Servlet_check_duplicate_file() {

        try {

            String urlstr = "http://" + Settings.serverIP + ":8084/PortalServer/CheckDuplicateFileServlet";
            URL url = new URL(urlstr);
            URLConnection connection = url.openConnection();

            connection.setDoOutput(true);
            connection.setDoInput(true);

            // don't use a cached version of URL connection
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // specify the content type that binary data is sent
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
            // send and serialize the object

            out.writeObject(libforDuplicateCheck);
            out.close();

            // define a new ObjectInputStream on the input stream
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
            // receive and deserialize the object, note the cast
            isDuplicateFile = (boolean) in.readObject();

            in.close();

        } catch (Exception e) {

            System.out.println("Error: " + e);
            e.printStackTrace();
        }

    }

    class MyUploadTask extends TimerTask {

        ManageDocumentFrm parent;
        int chunkSize;
        boolean lastChunk = false;
        boolean done = false;

        boolean end = false;
        public byte data[];

        MyUploadTask(ManageDocumentFrm parent) {
            this.parent = parent;

        }

        @Override
        public void run() {
            if (parent.running) {
                try {
                    FileInputStream fin = new FileInputStream(new File(parent.path));
                    int available = fin.available();
                    chunkSize = available / 1024;
                    int residualSize = available - (chunkSize * 1024);
                    if (residualSize > 0) {
                        chunkSize++;
                    }
                    parent.jProgressBar1.setMaximum(chunkSize);
                    for (int i = 0; i < chunkSize; i++) {
                        lib = new DataLib();
                        lib.name = parent.name;
                        lib.userId = Settings.UserId;
                        lib.fileHash = fileHash;
                        // lib.userId = allClassDetails.get(1).classroomid;
                        if (available > 1024) {
                            data = new byte[1024];
                            available -= 1024;
                            fin.read(data);
                            lib.data = data;
                            lib.flag = false;
                            call_Servlet();
                        } else {
                            System.out.println("data size is:" + fin.available());
                            data = new byte[available];
                            lastChunk = true;
                            fin.read(data);
                            lib.data = data;
                            lib.flag = true;
                            done = call_Servlet();
                        }

                        parent.jProgressBar1.setValue(i + 1);
                        Thread.sleep(10);
                    }
                    fin.close();
                    if (done) {
                        end = true;
                        JOptionPane.showMessageDialog(parent, "File Uploaded Successfully");
                        parent.running = false;
                        isDuplicateFile = false;
                        parent.jProgressBar1.setValue(0);
                    } else {
                        JOptionPane.showMessageDialog(parent, "File Uploading Failed!");
                        parent.running = false;

                    }
                } catch (Exception e) {
                    System.out.println("error in timer task" + e);
                }
            }
        }

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jProgressBar1 = new javax.swing.JProgressBar();
        jPanel3 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jButton4 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(134, 58, 150));

        jPanel2.setBackground(new java.awt.Color(134, 58, 150));
        jPanel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));

        jLabel1.setForeground(new java.awt.Color(255, 218, 0));
        jLabel1.setText("My Document List");

        jPanel3.setBackground(new java.awt.Color(134, 58, 150));
        jPanel3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 218, 0)));

        jButton1.setBackground(new java.awt.Color(255, 218, 0));
        jButton1.setText("Choose Document to Upload");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton4.setBackground(new java.awt.Color(255, 218, 0));
        jButton4.setText("Upload Document");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jButton2.setBackground(new java.awt.Color(255, 218, 0));
        jButton2.setText("Back");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextField1)
                    .addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addGap(0, 256, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 90, Short.MAX_VALUE)
                .addComponent(jButton4)
                .addGap(18, 18, 18)
                .addComponent(jButton2)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jProgressBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addComponent(jLabel1)
                            .addGap(363, 363, 363)))
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(47, 47, 47)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        try {
            FileDialog fd = new FileDialog(this, "select File", FileDialog.LOAD);
            fd.setVisible(true);
            path = fd.getDirectory() + fd.getFile();
            name = fd.getFile();
            fileHash = getSHA(path);
            jTextField1.setText(name);
            libforDuplicateCheck = new DataLib();
            libforDuplicateCheck.fileHash = fileHash;
            libforDuplicateCheck.userId = Settings.UserId;
            call_Servlet_check_duplicate_file();
        } catch (Exception e) {
            System.out.println("error in selecting file" + e);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
        this.setVisible(false);
        new MainFrm().setVisible(true);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        // TODO add your handling code here:
        String m = JOptionPane.showInputDialog("Please enter your session password");
        System.out.println("Password: " + m);
        if (m.trim().equals(Settings.sessionPassword)) {
            if (isDuplicateFile) {
                JOptionPane.showMessageDialog(this, "File already present on server");
                return;
            } else {
                running = true;
            }

        } else {
            JOptionPane.showMessageDialog(this, "Session password mismatch");
        }

    }//GEN-LAST:event_jButton4ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    public javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
}
