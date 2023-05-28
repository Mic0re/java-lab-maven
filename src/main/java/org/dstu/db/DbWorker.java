package org.dstu.db;

import org.dstu.util.CsvReader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class DbWorker {
    public static void populateFromFile(String fileName) {
        List<String[]> strings = CsvReader.readCsvFile(fileName, ";");
        Connection conn = DbConnection.getConnection();
        try {
            Statement cleaner = conn.createStatement();
            System.out.println(cleaner.executeUpdate("DELETE FROM ship"));
            System.out.println(cleaner.executeUpdate("DELETE FROM airplane"));
            PreparedStatement airplaneSt = conn.prepareStatement(
                    "INSERT INTO airplane (brand, model, number_of_seats) " +
                            "VALUES (?, ?, ?)");
            PreparedStatement shipSt = conn.prepareStatement(
                    "INSERT INTO ship (brand, model, max_cargo)" +
                            "VALUES (?, ?, ?)");

            for (String[] line: strings) {
                if (line[0].equals("0")) {
                    shipSt.setString(1, line[1]);
                    shipSt.setString(2, line[2]);
                    shipSt.setInt(3, Integer.parseInt(line[3]));
                    shipSt.addBatch();
                } else {
                    airplaneSt.setString(1, line[1]);
                    airplaneSt.setString(2, line[2]);
                    airplaneSt.setInt(3, Integer.parseInt(line[3]));
                    airplaneSt.addBatch();
                }
            }
            int[] airplaneRes = airplaneSt.executeBatch();
            int[] shipRes = shipSt.executeBatch();
            for (int num: airplaneRes) {
                System.out.println(num);
            }

            for (int num: shipRes) {
                System.out.println(num);
            }
            cleaner.close();
            airplaneSt.close();
            shipSt.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void demoQuery() {
        Connection conn = DbConnection.getConnection();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM airplane WHERE number_of_seats > 60");
            while (rs.next()) {
                System.out.print(rs.getString("brand"));
                System.out.print(" ");
                System.out.print(rs.getString("model"));
                System.out.print(" ");
                System.out.println(rs.getString("number_of_seats"));
            }
            rs.close();
            st.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void dirtyReadDemo() {
        Runnable first = () -> {
            Connection conn1 = DbConnection.getNewConnection();
            if (conn1 != null) {
                try {
                    conn1.setAutoCommit(false);
                    conn1.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                    Statement upd = conn1.createStatement();
                    upd.executeUpdate("UPDATE ship SET max_cargo=4000 WHERE model='Emma Mærsk'");
                    Thread.sleep(2000);
                    conn1.rollback();
                    upd.close();
                    Statement st = conn1.createStatement();
                    System.out.println("In the first thread:");
                    ResultSet rs = st.executeQuery("SELECT * FROM ship");
                    while (rs.next()) {
                        System.out.println(rs.getString("max_cargo"));
                    }
                    st.close();
                    rs.close();
                    conn1.close();
                } catch (SQLException | InterruptedException throwables) {
                    throwables.printStackTrace();
                }
            }
        };

        Runnable second = () -> {
            Connection conn2 = DbConnection.getNewConnection();
            if (conn2 != null) {
                try {
                    Thread.sleep(500);
                    conn2.setAutoCommit(false);
                    conn2.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                    Statement st = conn2.createStatement();
                    ResultSet rs = st.executeQuery("SELECT * FROM ship");
                    while (rs.next()) {
                        System.out.println(rs.getString("max_cargo"));
                    }
                    rs.close();
                    st.close();
                    conn2.close();
                } catch (SQLException | InterruptedException throwables) {
                    throwables.printStackTrace();
                }
            }
        };
        Thread th1 = new Thread(first);
        Thread th2 = new Thread(second);
        th1.start();
        th2.start();
    }
}
