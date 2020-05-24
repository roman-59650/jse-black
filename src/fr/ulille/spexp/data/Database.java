
package fr.ulille.spexp.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ulille.spexp.compiler.CompilationData;
import fr.ulille.spexp.compiler.MiscData;
import fr.ulille.spexp.fx.Main;
import fr.ulille.spexp.spectrum.FileInfo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import org.apache.commons.io.FileUtils;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;

/**
 *
 * @author Roman Motiyenko
 */
public class Database {

    private static final String defaultDbFormat = "i:J,i:Ka,i:Kc";

    private String userHomeDir;
    private String systemDir;
    private String dbName;
    private String driver = "org.apache.derby.jdbc.EmbeddedDriver";
    private String protocol = "jdbc:derby";
    private Connection conn;
    private Properties props;
//*****************************
// strings
    private String insertDbfString = "INSERT INTO APP.DBFORMAT "+
                                  "(QN, FORM)"+ 
                                  " VALUES (?, ?)";            
    private String insertString = "INSERT INTO APP.PEAK "+ 
                                  "(PEAK_FR, GAUSS_FR, VOIGT_FR, STATUS, REFS)"+
                                  " VALUES (?, ?, ?, ?, ?)";
    private String getRowString = "SELECT COUNT(*) FROM APP.PEAK WHERE PEAK_FR > ? AND PEAK_FR < ? ";
    private String getFilterString = "SELECT * FROM APP.PEAK WHERE "+
                                     "PEAK_FR > ? AND PEAK_FR < ? "+
                                     "ORDER BY PEAK_FR";
    private String clearSpString = "DELETE FROM APP.SPDATA";
    private String insertSpString;
    private String assignSpString;
    private String insertPrString = "INSERT INTO APP.PRDATA "
                                    + "(CFREQ, AMAX, COLOR, NLINES, REFS) "
                                    + "VALUES (?, ?, ?, ?, ?)";
    private String filterPrString = "SELECT * FROM APP.PRDATA WHERE "
                                    + "CFREQ > ? AND CFREQ < ? "
                                    + "ORDER BY CFREQ";
    private String updateAssignment = "UPDATE APP.PEAK "
                                    + "SET STATUS = ?, REFS = ? "
                                    + "WHERE ID = ?";
    private String updateFrequency = "UPDATE APP.PEAK "
                                    + "SET GAUSS_FR = ? "
                                    + "WHERE ID = ?";
    private String getAsgnTable = "SELECT * FROM APP.AODATA "
                                    + "ORDER BY ID";
    private String getAsgnData = "SELECT * FROM APP.AODATA "
                                    + "WHERE SPECIES = ? "
                                    + "ORDER BY ID";
    private String insertCompDataString = "INSERT INTO APP.COMPDATA "
                                    + "(FILENAME, ALIAS, INTENSITY, COLOR) "
                                    + "VALUES(?, ?, ?, ?)";
    private String insertMiscDataString = "INSERT INTO APP.MISC "
                                    +"(SPPATH, TEMP, MASS, QFUNC, ICOFF) "
                                    +"VALUES(?, ?, ?, ?, ?)";
    private String insertFilesDataString = "INSERT INTO APP.FILES " +
                                    "(FILENAME, MINFREQ, MAXFREQ) " +
                                    "VALUES(?, ?, ?)";
    private String getFileFreqString = "SELECT * FROM APP.FILES "
                                    + "WHERE MINFREQ < ? AND MAXFREQ > ? ";
    private String updateIntensity = "UPDATE APP.AODATA "
                                    + "SET INTENS = ? "
                                    + "WHERE ID = ?";
    private String deleteRow = "DELETE FROM APP.AODATA " +
                                "WHERE ID = ?";
    private String deletePeak = "DELETE FROM APP.PEAK " +
                                "WHERE ID = ?";


    private List<String> aliasList;
    private List<String> qnumsList;
    private List<String> selruList;

//*****************************
// prepared statements
    private PreparedStatement insertDbfStatement;
    private PreparedStatement insertStatement;
    private PreparedStatement getRowStatement;
    private PreparedStatement getFilterStatement;
    private PreparedStatement getPeakIntervalStatement;
    private PreparedStatement insertSpStatement;
    private PreparedStatement clearSpTableStatement;
    private PreparedStatement assignSpStatement;
    private PreparedStatement insertPrStatement;
    private PreparedStatement filterPrData;
    private PreparedStatement updAsgnStatement;
    private PreparedStatement getAsgnStatement;
    private PreparedStatement getAsgnDataList;
    private PreparedStatement updFreqStatement;
    private PreparedStatement insertCompDataStatement;
    private PreparedStatement insertMiscDataStatement;
    private PreparedStatement insertFilesDataStatement;
    private PreparedStatement getFileFreqStatetment;
    private PreparedStatement updRelIntStatement;
    private PreparedStatement deleteRowStatement;
    private PreparedStatement deletePeakStatement;
//*****************************    
    /* Project datasets */
    private ResultSet globrs; // peaks
    public ResultSet predrs;  // predictions
    public ResultSet tranrs;  // compiled predictions
    public ResultSet asgnrs;  // assignment
    public ResultSet comprs;  // compilation data
    public ResultSet miscrs;  // miscellaneous data
    private boolean isConn = false;
    private DbFormat dbformat;
    private CachedRowSet crs;

    public CachedRowSet getPeaksCache(){
        return crs;
    }

    public ResultSet getPeakResultSet(){
        return globrs;
    }

    private void setDBSystemDir() {
        // decide on the db system directory
        userHomeDir = System.getProperty("user.dir");
        systemDir = userHomeDir + "/db";
        System.setProperty("derby.system.home", systemDir);
        // create the db system directory
        File fileSystemDir = new File(systemDir);
        if (!fileSystemDir.exists()) fileSystemDir.mkdir();
        /*File tempDir = new File(systemDir+"/_temp");
        if (tempDir.exists()) {
            try {
                Files.walk(tempDir.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
    }

    public void deleteDirectory(Path pathToBeDeleted) throws IOException {
        Files.walk(pathToBeDeleted)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public void deleteDatabase(String dbname){
        try {
            this.closeConnection();
            FileUtils.deleteDirectory(new File(systemDir+"/"+dbname));
            //deleteDirectory(new File(systemDir+"/"+dbname).toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Database(String dataBase){
        dbName = dataBase;
        setDBSystemDir();
        dbformat = new DbFormat(defaultDbFormat);
        aliasList = new ArrayList<>();
        qnumsList = new ArrayList<>();
        selruList = new ArrayList<>();
    }

    public String getDbName(){
        return dbName;
    }

    /**
     * Sets the connection to a database. Prepares all DB statetments.
     * If the database does not exist, creates its tables
     */
    public void setConnection(){
        try {
            props = new Properties();
            File testDir = new File(systemDir+"/"+dbName);
            boolean exst = testDir.exists();
            if (!exst) props.put("create","true");
            // connecting the database driver
            conn = DriverManager.getConnection(protocol+':'+dbName, props);
            if (!conn.isClosed()) isConn = true;
            else isConn=false;

            // creating tables if necessary
            if (!exst) {
                createTables();
                insertDbfStatement = conn.prepareStatement(insertDbfString);
                insertDbFormat(dbformat);
            }
            else{
                dbformat = getDbFormat();
            }
            // preparing statements
            insertStatement = conn.prepareStatement(insertString);
            insertCompDataStatement = conn.prepareStatement(insertCompDataString);
            insertMiscDataStatement = conn.prepareStatement(insertMiscDataString);
            insertFilesDataStatement = conn.prepareStatement(insertFilesDataString);
            getRowStatement = conn.prepareStatement(getRowString);
            getFilterStatement = conn.prepareStatement(
                                           getFilterString,
                                           ResultSet.TYPE_SCROLL_INSENSITIVE,
                                           ResultSet.CONCUR_READ_ONLY);
            getPeakIntervalStatement = conn.prepareStatement(
                    getFilterString,
                    ResultSet.TYPE_SCROLL_SENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            clearSpTableStatement = conn.prepareStatement(clearSpString);
            updRelIntStatement = conn.prepareStatement(updateIntensity);
            deleteRowStatement = conn.prepareStatement(deleteRow);
            deletePeakStatement = conn.prepareStatement(deletePeak);
            String strprep = "";
            String strval = "";
            for (int j=2;j>=1;j--)
                for (int i=0;i<dbformat.getLength();i++){
                    strprep = strprep + dbformat.getQName(i)+String.valueOf(j)+", ";
                    strval = strval+"?, ";
                }
            insertSpString = "INSERT INTO APP.SPDATA "
                    + "(SPECIES, "
                    + strprep
                    + "FREQ, ALPHA, COLOR) "
                    + "VALUES (?, "
                    + strval
                    + "?, ?, ?)";
            assignSpString = "INSERT INTO APP.AODATA "
                    + "(SPECIES, "
                    + strprep
                    + "AFREQ, WEIGHT, INTENS) "
                    + "VALUES (?, "
                    + strval
                    + "?, ?, ?)";
            insertSpStatement = conn.prepareStatement(insertSpString);
            insertPrStatement = conn.prepareStatement(insertPrString);
            assignSpStatement = conn.prepareStatement(assignSpString);
            filterPrData = conn.prepareStatement(filterPrString,
                                           ResultSet.TYPE_SCROLL_INSENSITIVE,
                                           ResultSet.CONCUR_UPDATABLE);
            updAsgnStatement = conn.prepareStatement(updateAssignment);
            updFreqStatement = conn.prepareStatement(updateFrequency);
            getAsgnStatement = conn.prepareStatement(getAsgnTable,
                                            ResultSet.TYPE_SCROLL_INSENSITIVE,
                                            ResultSet.CONCUR_UPDATABLE);
            getAsgnDataList = conn.prepareStatement(getAsgnData,
                                            ResultSet.TYPE_SCROLL_INSENSITIVE,
                                            ResultSet.CONCUR_UPDATABLE);
            getFileFreqStatetment = conn.prepareStatement(getFileFreqString,
                                             ResultSet.TYPE_SCROLL_INSENSITIVE,
                                             ResultSet.CONCUR_UPDATABLE);
            globrs = null;
            predrs = null;
            tranrs = null;
            asgnrs = null;
            comprs = null;
            miscrs = null;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Connection getConnection(){
        return conn;
    }

    public void closeConnection(){
        try {
            conn.close();
            isConn = false;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void closeConnectionAll(){
        try {
            conn.close();
            conn = DriverManager.getConnection(protocol+':'+dbName+";shutdown=true");
            isConn = false;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public List<String> getAliasList(){
        aliasList.clear();
        String statement = "SELECT * FROM APP.COMPDATA ORDER BY ID";
        Statement s = null;
        try {
            s = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet res = s.executeQuery(statement);
            if (res.first()) aliasList.add(res.getString("ALIAS"));
            while(res.next()) aliasList.add(res.getString("ALIAS"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return aliasList;
    }

    public List<String> getQnumsList() {
        qnumsList.clear();                      // to think about generating q.n. list during db connection phase
        for (int j = 2; j >= 1; j--)            // here we need only to return the list
            for (int i = 0; i < dbformat.getLength(); i++)
                qnumsList.add(dbformat.getQName(i) + j);
        return qnumsList;
    }

    public List<String> getSelruList(){
        selruList.clear();
        for (int i = 0; i < dbformat.getLength(); i++)
            selruList.add(dbformat.getQName(i) + "2-"+ dbformat.getQName(i)+"1");
        return selruList;
    }

    private void createTables(){
        try {
            String createCompString = "CREATE TABLE APP.COMPDATA "
                    +"(ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY "
                    + "(START WITH 1, INCREMENT BY 1), "
                    + "FILENAME VARCHAR(256), "
                    + "ALIAS VARCHAR(32), "
                    + "INTENSITY DOUBLE, "
                    + "COLOR INT)";
            java.sql.Statement s = conn.createStatement();
            s.execute(createCompString);

        String createDbfString = "CREATE TABLE APP.DBFORMAT "
                + "(ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY "
                + "(START WITH 1, INCREMENT BY 1), "
                + "QN VARCHAR(10), "
                + "FORM VARCHAR(2))";
        s.execute(createDbfString);
                     
        String createString = "CREATE TABLE APP.PEAK "
                + "(ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY, "
                + "PEAK_FR DOUBLE, "
                + "VOIGT_FR DOUBLE, "
                + "GAUSS_FR DOUBLE,"
                + "STATUS BOOLEAN,"
                + "REFS VARCHAR(512))";
        s.execute(createString);

        String str = "";
        for (int j=2;j>=1;j--)
            for (int i=0;i<dbformat.getLength();i++){
                str = str + dbformat.getQName(i)+String.valueOf(j)+' ';
                if (dbformat.getDataType(i)==DbFormat.qnDataType.IntData)
                    str = str+"INT, ";
                if (dbformat.getDataType(i)==DbFormat.qnDataType.FloData)
                    str = str+"DOUBLE, ";
                if (dbformat.getDataType(i)==DbFormat.qnDataType.StrData)
                    str = str+"VARCHAR(6), ";
            }
        String createIdsString = "CREATE TABLE APP.SPDATA "
                + "(ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY"
                + "(START WITH 1, INCREMENT BY 1), "
                + "SPECIES VARCHAR(20), "
                + str
                + "FREQ DOUBLE, "
                + "ALPHA DOUBLE, "
                + "COLOR INT)";
        s.execute(createIdsString);

        String createPdsString = "CREATE TABLE APP.PRDATA "
                + "(CFREQ DOUBLE, "
                + "AMAX DOUBLE, "
                + "COLOR INT, "
                + "NLINES INT, "
                + "REFS VARCHAR(512))";
        s.execute(createPdsString);

        String createOdsString = "CREATE TABLE APP.AODATA "
                + "(ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY, "
                + "SPECIES VARCHAR(30), "
                + str
                + "AFREQ DOUBLE, "
                + "WEIGHT DOUBLE, "
                + "INTENS DOUBLE, "
                + "COMMENT VARCHAR(30))";
        s.execute(createOdsString);

        String creatMiscString = "CREATE TABLE APP.MISC "+
                "(SPPATH VARCHAR(256), "+
                "TEMP DOUBLE, "+
                "MASS DOUBLE, "+
                "QFUNC DOUBLE, " +
                "ICOFF DOUBLE)";
        s.execute(creatMiscString);

        String createFilesString = "CREATE TABLE APP.FILES "+
                "(FILENAME VARCHAR(256), " +
                "MINFREQ DOUBLE, " +
                "MAXFREQ DOUBLE)";
        s.execute(createFilesString);

        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void insertDbFormat(DbFormat dbf){
        Statement sd;
        try {
            sd = conn.createStatement();
            sd.execute("DELETE FROM APP.DBFORMAT");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        dbformat = dbf;
        int len = dbf.getLength();
        for (int i=0;i<len;i++){
            try {
                insertDbfStatement.setString(1, dbf.getQName(i));
                if (dbf.getDataType(i)==DbFormat.qnDataType.IntData)
                    insertDbfStatement.setString(2, "i");
                else
                if (dbf.getDataType(i)==DbFormat.qnDataType.FloData)
                    insertDbfStatement.setString(2, "d");
                else                    
                if (dbf.getDataType(i)==DbFormat.qnDataType.StrData)
                    insertDbfStatement.setString(2, "s");
                insertDbfStatement.executeUpdate();    
            } catch (SQLException ex) {
                Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void insertValue(double pvalue, double gvalue){
        try {
            insertStatement.setDouble(1, pvalue);
            insertStatement.setDouble(2, gvalue);
            insertStatement.setDouble(3, 0.0);
            insertStatement.setBoolean(4, false);
            insertStatement.setString(5, "");
            insertStatement.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void insertSpValue(String field, Object value){
        try {
            String str = "INSERT INTO APP.SPDATA "
                    + "(" + field + ")"
                    + "VALUES (" + value.toString() + ")";
            java.sql.Statement s = conn.createStatement();
            s.execute(str);
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void insertPDataRow(PredictRowData row){
        try {
            insertSpStatement.setString(1, row.getSpecies());
            int qnslength = row.getQnsLength();
            for (int i = 0; i < qnslength; i++) {
                DbFormat.qnDataType qtp = dbformat.getDataType(i);
                if (qtp==DbFormat.qnDataType.IntData)
                    insertSpStatement.setInt(i + 2, (Integer) row.getQns(i));
                else
                if (qtp==DbFormat.qnDataType.FloData)
                    insertSpStatement.setDouble(i + 2, (Double) row.getQns(i));
                else
                if (qtp==DbFormat.qnDataType.StrData)
                    insertSpStatement.setString(i + 2, (String) row.getQns(i));                
            }
            insertSpStatement.setDouble(qnslength + 2, row.getFrequency());
            insertSpStatement.setDouble(qnslength + 3, row.getIntensity());
            insertSpStatement.setInt(qnslength + 4, row.getColor());
            insertSpStatement.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int insertADataRow(PredictRowData row,
                                double freq, double weight, double rint){
        int rowId = -1;
        try {
            assignSpStatement.setString(1, row.getSpecies());
            int qnslength = row.getQnsLength();
            for (int i = 0; i < qnslength; i++) {
                if (dbformat.getDataType(i)==DbFormat.qnDataType.IntData)
                    assignSpStatement.setInt(i + 2, (Integer) row.getQns(i));
                else
                if (dbformat.getDataType(i)==DbFormat.qnDataType.FloData)
                    assignSpStatement.setDouble(i + 2, (Double) row.getQns(i));
                else
                if (dbformat.getDataType(i)==DbFormat.qnDataType.StrData)
                    assignSpStatement.setString(i + 2, (String) row.getQns(i)); 
            }
            assignSpStatement.setDouble(qnslength + 2, freq);
            assignSpStatement.setDouble(qnslength + 3, weight);
            assignSpStatement.setDouble(qnslength + 4, rint);
            assignSpStatement.executeUpdate();

            asgnrs = getAsgnStatement.executeQuery();
            asgnrs.last();
            rowId = asgnrs.getInt("ID");
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        return rowId;
    }

    public int getRowCount(double minfreq, double maxfreq){
        int result = -1;
        try {
            ResultSet rs = null;
            getRowStatement.setDouble(1,minfreq);
            getRowStatement.setDouble(2,maxfreq);
            rs = getRowStatement.executeQuery();
            rs.next();
            result = rs.getInt(1);
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public PredictRowData getPredictedDataRow(int pos){
        PredictRowData row = new PredictRowData(dbformat);
        try {
            tranrs.absolute(pos);
            row.setId(tranrs.getInt("ID"));
            row.setSpecies(tranrs.getString("SPECIES"));
            for (int i = 0; i < dbformat.getLength(); i++) {
                if (dbformat.getDataType(i) == DbFormat.qnDataType.IntData) {
                    row.setQns(i, tranrs.getInt(i+3));
                }
                if (dbformat.getDataType(i) == DbFormat.qnDataType.StrData) {
                    row.setQns(i, tranrs.getString(i+3));
                }
                if (dbformat.getDataType(i) == DbFormat.qnDataType.FloData) {
                    row.setQns(i, tranrs.getDouble(i+3));
                }                
            }
            int len = dbformat.getLength();
            for (int i = 0; i < dbformat.getLength(); i++) {
                if (dbformat.getDataType(i) == DbFormat.qnDataType.IntData) {
                    row.setQns(i+len, tranrs.getInt(i+3+len));
                }
                if (dbformat.getDataType(i) == DbFormat.qnDataType.StrData) {
                    row.setQns(i+len, tranrs.getString(i+3+len));
                }
                if (dbformat.getDataType(i) == DbFormat.qnDataType.FloData) {
                    row.setQns(i+len, tranrs.getDouble(i+3+len));
                }                
            }
            row.setFrequency(tranrs.getDouble("FREQ"));
            row.setIntensity(tranrs.getDouble("ALPHA"));
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally{
            return row;
        }
    }

    public PredictRowData _findTransition(Transition transition){
        PredictRowData row = new PredictRowData(dbformat);
        List<String> qnumsList = getQnumsList();
        String statement = "SELECT * FROM APP.SPDATA WHERE ";
        ResultSet res = null;
        for (int i=0;i<qnumsList.size()-1;i++) {
            statement = statement + qnumsList.get(i) + "=" + transition.getQns(i) + " AND ";
        }
        statement = statement + qnumsList.get(qnumsList.size()-1) + "=" + transition.getQns(qnumsList.size()-1);
        System.out.println(statement);
        Statement s = null;
        try {
            s = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            res = s.executeQuery(statement);
            res.first();
            row.setId(res.getInt("ID"));
            row.setSpecies(res.getString("SPECIES"));
            for (int i = 0; i < dbformat.getLength(); i++) {
                if (dbformat.getDataType(i) == DbFormat.qnDataType.IntData) {
                    row.setQns(i, res.getInt(i+3));
                }
                if (dbformat.getDataType(i) == DbFormat.qnDataType.StrData) {
                    row.setQns(i, res.getString(i+3));
                }
                if (dbformat.getDataType(i) == DbFormat.qnDataType.FloData) {
                    row.setQns(i, res.getDouble(i+3));
                }
            }
            int len = dbformat.getLength();
            for (int i = 0; i < dbformat.getLength(); i++) {
                if (dbformat.getDataType(i) == DbFormat.qnDataType.IntData) {
                    row.setQns(i+len, res.getInt(i+3+len));
                }
                if (dbformat.getDataType(i) == DbFormat.qnDataType.StrData) {
                    row.setQns(i+len, res.getString(i+3+len));
                }
                if (dbformat.getDataType(i) == DbFormat.qnDataType.FloData) {
                    row.setQns(i+len, res.getDouble(i+3+len));
                }
            }
            row.setFrequency(res.getDouble("FREQ"));
            row.setIntensity(res.getDouble("ALPHA"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return row;
    }

    public AssignRowData getADataRow(int pos){
        AssignRowData row = new AssignRowData(dbformat);
        try {
            String statement = "SELECT * FROM APP.AODATA WHERE ID = "+pos;
            Statement s = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet res = s.executeQuery(statement);
            if (!res.first()) return row;
            row.setSpecies(res.getString("SPECIES"));
            for (int i = 0; i < dbformat.getLength(); i++) {
                String qn = dbformat.getQName(i)+"2";
                if (dbformat.getDataType(i) == DbFormat.qnDataType.IntData) {
                    row.setQns(i, res.getInt(qn));
                }
                if (dbformat.getDataType(i) == DbFormat.qnDataType.StrData) {
                    row.setQns(i, res.getString(qn));
                }
                if (dbformat.getDataType(i) == DbFormat.qnDataType.FloData) {
                    row.setQns(i, res.getDouble(qn));
                }                
            }
            int len = dbformat.getLength();
            for (int i = 0; i < dbformat.getLength(); i++) {
                String qn = dbformat.getQName(i)+"1";
                if (dbformat.getDataType(i) == DbFormat.qnDataType.IntData) {
                    row.setQns(i+len, res.getInt(qn));
                }
                if (dbformat.getDataType(i) == DbFormat.qnDataType.StrData) {
                    row.setQns(i+len, res.getString(qn));
                }
                if (dbformat.getDataType(i) == DbFormat.qnDataType.FloData) {
                    row.setQns(i+len, res.getDouble(qn));
                }                
            }
            row.setFrequency(res.getDouble("AFREQ"));
            row.setWeight(res.getDouble("WEIGHT"));
            row.setIntensity(res.getDouble("INTENS"));
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        return row;
    }
    
    public void getPeakData(double fmin, double fmax){
        try {
            getFilterStatement.setDouble(1, fmin);
            getFilterStatement.setDouble(2, fmax);
            globrs = getFilterStatement.executeQuery();

            RowSetFactory aFactory = RowSetProvider.newFactory();
            crs = aFactory.createCachedRowSet();
            crs.populate(globrs);
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void getPredList(double fmin, double fmax){
        try {
            filterPrData.setDouble(1, fmin);
            filterPrData.setDouble(2, fmax);
            predrs = filterPrData.executeQuery();
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void getTransList(String filter){
        try {
            String statement = "SELECT * FROM APP.SPDATA "+filter;
            java.sql.Statement s = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            tranrs = s.executeQuery(statement);
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void getAsgnList(){
        try {
            asgnrs = getAsgnStatement.executeQuery();
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public List<String> getAssignedLineList(String species, int format){
        List<String> list = new ArrayList<>();
        ResultSet rs = null;
        try{
            String statement = "SELECT * FROM APP.PEAK WHERE STATUS=TRUE";
            java.sql.Statement sql = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = sql.executeQuery(statement);
            while (rs.next()){
                if (rs.getBoolean("STATUS")){
                    String s = rs.getString("REFS");
                    if (s.trim().isEmpty()) continue;
                    String[] ss = s.split(",");
                    int[] refs = Arrays.stream(ss).mapToInt(Integer::parseInt).toArray();
                    for (int ref : refs) {
                        AssignRowData row = this.getADataRow(ref);
                        if (row.getSpecies().equals(species)){
                            row.setFormat(format);
                            list.add(row.toString());
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            rs.close();
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        return list;
    }
    
    public List<String> getAsgnLineList(String species, int format){
        List<String> list = new ArrayList<>();
        try {
            getAsgnDataList.setString(1, species);
            asgnrs = getAsgnDataList.executeQuery();
            AssignRowData arow;
            if (asgnrs.first()) {
                arow = getADataRow(asgnrs.getInt("ID"));
                arow.setFormat(format);
                list.add(arow.toString());
            } else return list;
            while (asgnrs.next()){
                arow = getADataRow(asgnrs.getInt("ID"));
                arow.setFormat(format);
                list.add(arow.toString());
            }

        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        return list;
    }    

    public boolean getPeakInterval(double freq, double interval){
        boolean result = false;
        try {
            getPeakIntervalStatement.setDouble(1, freq - interval / 2.);
            getPeakIntervalStatement.setDouble(2, freq + interval / 2.);
            ResultSet rs = getPeakIntervalStatement.executeQuery();
            if (rs.next()) result = true;
            }
        catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public ResultSet getPeakAtFrequency(double freq){
        ResultSet rs = null;
        MiscData misc = getMiscData();
        double doppler = 2*3.58e-7*freq*Math.sqrt(misc.getTemp()/misc.getMass());
        try {
            getPeakIntervalStatement.setDouble(1, freq - doppler);
            getPeakIntervalStatement.setDouble(2, freq + doppler);
            rs = getPeakIntervalStatement.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rs;
    }

    public PeakData _getPeakAtRow(int row){
        ArrayList<Integer> list = new ArrayList<>();
        PeakData data = new PeakData(-1,0,0,false, list);
        try {
            globrs.absolute(row);
            String s = globrs.getString("REFS");
            data.setStringReferences(s);
            String[] ss = {};
            if (!s.isEmpty()) ss = s.split(",");
            for (int i=0;i<ss.length;i++)
                list.add(Integer.parseInt(ss[i]));

            data.setId(globrs.getInt("ID"));
            data.setPeakfrequency(globrs.getDouble("PEAK_FR"));
            data.setFittedfrequency(globrs.getDouble("GAUSS_FR"));
            data.setAssigned(globrs.getBoolean("STATUS"));
            data.setReferences(list);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            return data;
        }
    }

    public PeakData _getPeakAtFrequency(double freq){
        ResultSet rs = null;
        MiscData misc = getMiscData();
        ArrayList<Integer> list = new ArrayList<>();
        PeakData data = new PeakData(-1,0,0,false, list);
        double doppler = 2*3.58e-7*freq*Math.sqrt(misc.getTemp()/misc.getMass());
        try {
            getPeakIntervalStatement.setDouble(1, freq - doppler);
            getPeakIntervalStatement.setDouble(2, freq + doppler);
            rs = getPeakIntervalStatement.executeQuery();
            rs.first();

            String s = rs.getString("REFS");
            data.setStringReferences(s);
            String[] ss = {};
            if (!s.isEmpty()) ss = s.split(",");
            for (int i=0;i<ss.length;i++)
                list.add(Integer.parseInt(ss[i]));

            data.setId(rs.getInt("ID"));
            data.setPeakfrequency(rs.getDouble("PEAK_FR"));
            data.setFittedfrequency(rs.getDouble("GAUSS_FR"));
            data.setAssigned(rs.getBoolean("STATUS"));
            data.setReferences(list);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            return data;
        }
    }

    public double getPeakFrequency(int row){
        double f = 0.0;
        try {
            globrs.absolute(row);
            f = globrs.getDouble("PEAK_FR");
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally {
            return f;
        }
    }

    public double getPeakFittedFrequency(int row){
        double f = 0;
        try {
            globrs.absolute(row);
            f = globrs.getDouble("GAUSS_FR");
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            return f;
        }
    }

    public boolean isConnected(){
        return isConn;
    }

    public void setDbFormat(DbFormat format){
        dbformat = format;
    }

    public DbFormat getDbFormat(){
        try {
            String statement = "SELECT * FROM APP.DBFORMAT";
            java.sql.Statement s = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet dbfrs = s.executeQuery(statement);
            dbfrs.first();
            String strform = "";
            strform = dbfrs.getString("FORM")+":"+dbfrs.getString("QN");
            while (dbfrs.next()) strform += ","+dbfrs.getString("FORM")+":"+dbfrs.getString("QN");
            dbformat.setFormat(strform);
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }        
        return dbformat;
    }

    public void setDbName(String name){
        dbName = name;
    }

    public void clearSpTable(){
        try {
            if (tranrs!=null) tranrs.close();
            if (predrs!=null) predrs.close();
            clearSpTableStatement.executeUpdate();
            java.sql.Statement s = conn.createStatement();
            s.execute("ALTER TABLE APP.SPDATA ALTER COLUMN ID RESTART WITH 1");
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int setCompiledPreds(String statement){
        int maxrow = 0;
        double unresolvedWidth = Double.parseDouble((Main.getProperties().getProperty("unres linewidth")));
        try {
            double tfr = 0., nfr = 0., over = 0., below = 0.;
            int col = 0, row, rowcount = 0;
            String rowstr = null;

            statement = "SELECT * FROM APP.SPDATA"+statement+ " ORDER BY FREQ";
            System.out.println(statement);
            java.sql.Statement s;
            s = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet res = s.executeQuery(statement);

            res.last();
            maxrow = res.getRow();
            System.out.println(maxrow);

            java.sql.Statement sd = conn.createStatement();
            sd.execute("DELETE FROM APP.PRDATA");

            if (res.first()){
                tfr = res.getDouble("FREQ");
                over = tfr*res.getDouble("ALPHA");
                below = res.getDouble("ALPHA");
                col = res.getInt("COLOR");
                rowcount = 1;
                row = res.getInt("ID");
                rowstr = String.valueOf(row);
            }
            long startTime = System.nanoTime();
            while (res.next()){
                nfr = res.getDouble("FREQ");
                if (nfr-tfr<unresolvedWidth){
                    over = over + nfr*res.getDouble("ALPHA");
                    below = below + res.getDouble("ALPHA");
                    col = col + res.getInt("COLOR");
                    rowcount++;
                    row = res.getInt("ID");
                    rowstr = rowstr+","+String.valueOf(row);
                }
                else{
                    if (below!=0){
                        insertPrStatement.setDouble(1, over/below);
                        insertPrStatement.setDouble(2, below);
                        insertPrStatement.setInt(3,col/rowcount);
                        insertPrStatement.setInt(4, rowcount);
                        insertPrStatement.setString(5, rowstr);
                        insertPrStatement.executeUpdate();
                        over = nfr*res.getDouble("ALPHA");
                        below = res.getDouble("ALPHA");
                        col = res.getInt("COLOR");
                        rowcount = 1;
                        row = res.getInt("ID");
                        rowstr = String.valueOf(row);
                    }
                }
                tfr = nfr;
            }
            long elapsedTime = System.nanoTime() - startTime;
            System.out.println("Total execution time: "+ elapsedTime/1000000);
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        return maxrow;
    }

    public void updatePeakAssignment(int id, String astr, boolean status){
        try {
            updAsgnStatement.setBoolean(1, status);
            updAsgnStatement.setString(2, astr);
            updAsgnStatement.setInt(3, id);
            updAsgnStatement.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void updatePeakFrequency(int id, double freq){
        try {
            updFreqStatement.setDouble(1,freq);
            updFreqStatement.setInt(2,id);
            updFreqStatement.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void updateRelativeIntensity(double relint, int ref){
        try {
            updRelIntStatement.setDouble(1, relint);
            updRelIntStatement.setInt(2, ref);
            updRelIntStatement.executeUpdate();
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void deleteAssignmentRow(int ref){
        try{
            deleteRowStatement.setInt(1, ref);
            deleteRowStatement.executeUpdate();
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void deleteSelectedPeak(int id){
        try{
            deletePeakStatement.setInt(1, id);
            deletePeakStatement.executeUpdate();
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    public ObservableList<CompilationData> getCompDataList(){
        ObservableList<CompilationData> list = FXCollections.observableArrayList();
        try {
            String statement = "SELECT * FROM APP.COMPDATA ORDER BY ID";
            Statement s = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet res = s.executeQuery(statement);
            if (res.first()) list.add(getCompData(res));
            while (res.next()) list.add(getCompData(res));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void setCompDataTable(ObservableList<CompilationData> list){
        Statement sd = null;
        try {
            sd = conn.createStatement();
            sd.execute("DELETE FROM APP.COMPDATA");
            for (int i=0;i<list.size();i++){
                insertCompDataStatement.setString(1,list.get(i).getPath());
                insertCompDataStatement.setString(2, list.get(i).getAlias());
                insertCompDataStatement.setDouble(3, list.get(i).getIntensity());
                insertCompDataStatement.setInt(4, list.get(i).getColor());
                insertCompDataStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private CompilationData getCompData(ResultSet rs) throws SQLException {
        CompilationData compilationData = new CompilationData("","", Color.BLACK,1,true);
        compilationData.setFilepath(rs.getString("FILENAME"));
        compilationData.setColor(rs.getInt("COLOR"));
        compilationData.setAlias(rs.getString("ALIAS"));
        compilationData.setIntensity(rs.getDouble("INTENSITY"));
        return compilationData;
    }

    public void updatePeak(int id, String astr, boolean status){
        try {
            updAsgnStatement.setBoolean(1, status);
            updAsgnStatement.setString(2, astr);
            updAsgnStatement.setInt(3, id);
            updAsgnStatement.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public MiscData getMiscData(){
        MiscData misc = new MiscData();
        Statement statement = null;
        try {
            statement = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            miscrs = statement.executeQuery("SELECT * FROM APP.MISC");
            if (miscrs.first()) {
                misc.setMass(miscrs.getDouble("MASS"));
                misc.setPath(miscrs.getString("SPPATH"));
                misc.setTemp(miscrs.getDouble("TEMP"));
                misc.setQfunc(miscrs.getDouble("QFUNC"));
                misc.setIcutoff(miscrs.getDouble("ICOFF"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return misc;
    }

    public void setMiscData(MiscData misc){
        Statement sd = null;
        try {
            sd = conn.createStatement();
            sd.execute("DELETE FROM APP.MISC");
            insertMiscDataStatement.setString(1, misc.getPath());
            insertMiscDataStatement.setDouble(2, misc.getTemp());
            insertMiscDataStatement.setDouble(3,misc.getMass());
            insertMiscDataStatement.setDouble(4,misc.getQfunc());
            insertMiscDataStatement.setDouble(5, misc.getIcutoff());
            insertMiscDataStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setFileData(List<FileInfo> info){
        Statement statement = null;
        try{
            statement = conn.createStatement();
            statement.execute("DELETE FROM APP.FILES");
            for (FileInfo i:info) {
                insertFilesDataStatement.setString(1,i.getFilename());
                insertFilesDataStatement.setDouble(2,i.getMinfrequency());
                insertFilesDataStatement.setDouble(3,i.getMaxfrequency());
                insertFilesDataStatement.executeUpdate();
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    public List<FileInfo> getFileInfo(){
        List<FileInfo> list = new ArrayList<>();
        try {
            String statement = "SELECT * FROM APP.FILES ORDER BY MINFREQ";
            Statement s = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet res = s.executeQuery(statement);
            if (res.first()) list.add(new FileInfo(res.getString("FILENAME"),
                                                    res.getDouble("MINFREQ"),
                                                    res.getDouble("MAXFREQ")));
            while (res.next()) list.add(new FileInfo(res.getString("FILENAME"),
                                                    res.getDouble("MINFREQ"),
                                                    res.getDouble("MAXFREQ")));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public String getFileAtFrequency(double frequency){
        String s = "";
        try {
            getFileFreqStatetment.setDouble(1,frequency);
            getFileFreqStatetment.setDouble(2,frequency);
            ResultSet rs = getFileFreqStatetment.executeQuery();
            if (rs.first()) s = rs.getString("FILENAME");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return s;
    }

    public ResultSet findTransition(Transition transition){
        List<String> qnumsList = getQnumsList();
        String statement = "SELECT * FROM APP.SPDATA WHERE ";
        ResultSet res = null;
        for (int i=0;i<qnumsList.size()-1;i++) {
            statement = statement + qnumsList.get(i) + "=" + transition.getQns(i) + " AND ";
        }
        statement = statement + qnumsList.get(qnumsList.size()-1) + "=" + transition.getQns(qnumsList.size()-1);
        System.out.println(statement);
        Statement s = null;
        try {
            s = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            res = s.executeQuery(statement);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    public int[] getPredictedReferences(int row){
        int[] refs = {};
        ArrayList<String> list = new ArrayList<>();
        try {
            predrs.absolute(row);
            String s = predrs.getString("REFS");
            String[] ss = s.split(",");
            Collections.addAll(list,ss);
            refs = new int[list.size()];
            for (int i=0;i<ss.length;i++)
                refs[i] = Integer.parseInt(ss[i]);
        } catch (SQLException e){
            e.printStackTrace();
        }
        return refs;
    }

    public int[] getAssignedReferences(int row){
        int[] refs = {};
        ArrayList<String> list = new ArrayList<>();
        try {
            globrs.absolute(row);
            String s = globrs.getString("REFS");
            String[] ss = s.split(",");
            Collections.addAll(list,ss);
            refs = new int[list.size()];
            for (int i=0;i<ss.length;i++)
                refs[i] = Integer.parseInt(ss[i]);
        } catch (SQLException e){
            e.printStackTrace();
        }
        return refs;
    }

    public String getAssignedReferencesAsString(int row){
        String str = "";
        try {
            crs.absolute(row);
            str = crs.getString("REFS");
        } catch (SQLException e){
            e.printStackTrace();
        }
        return str;
    }

}
