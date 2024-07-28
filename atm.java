import java.io.*;
import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;
public class atm {

    private static Connection connect() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/atm"; // Nama database Anda
        String user = "root"; // Username database
        String password = ""; // Password database
        return DriverManager.getConnection(url, user, password);
    }

    public class HashGenerator {
        public static void main(String[] args) {
            String password = "your_password"; // Ganti dengan password asli Anda
            String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
            System.out.println("Hashed password: " + hashed);
        }
    }
    private static boolean authenticate(String username, String password) {
        try (Connection conn = connect()) {
            String sql = "SELECT COUNT(*) FROM users WHERE username = ? AND password = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean registerUser(String username, String password) {
        try (Connection conn = connect()) {
            // Periksa apakah username sudah ada
            String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
            PreparedStatement checkPstmt = conn.prepareStatement(checkSql);
            checkPstmt.setString(1, username);
            ResultSet rs = checkPstmt.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                System.out.println("Username sudah ada.");
                return false;
            }

            // Tambah pengguna baru
            String insertSql = "INSERT INTO users (username, password) VALUES (?, ?)";
            PreparedStatement insertPstmt = conn.prepareStatement(insertSql);
            insertPstmt.setString(1, username);
            insertPstmt.setString(2, password);
            int rowsAffected = insertPstmt.executeUpdate();

            // Buat entri untuk saldo awal di tabel balances
            if (rowsAffected > 0) {
                String selectIdSql = "SELECT id FROM users WHERE username = ?";
                PreparedStatement selectIdPstmt = conn.prepareStatement(selectIdSql);
                selectIdPstmt.setString(1, username);
                ResultSet userIdRs = selectIdPstmt.executeQuery();
                userIdRs.next();
                int userId = userIdRs.getInt("id");

                String insertBalanceSql = "INSERT INTO balances (user_id, balance) VALUES (?, ?)";
                PreparedStatement insertBalancePstmt = conn.prepareStatement(insertBalanceSql);
                insertBalancePstmt.setInt(1, userId);
                insertBalancePstmt.setInt(2, 50000); // Saldo awal
                insertBalancePstmt.executeUpdate();

                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void recordTransaction(int userId, String type, int amount) {
        try (Connection conn = connect()) {
            String sql = "INSERT INTO transaction_history (user_id, type, amount) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setString(2, type);
            pstmt.setInt(3, amount);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        int pilihan, pilihan2, tabung, ambil, isi, index = 0, index2 = 0;
        int[] tambah_saldo = new int[20], tarik_saldo = new int[20];
        int biaya_pengiriman = 5000; // Biaya tambahan untuk pengiriman berbeda bank

        while (true) {
            System.out.println("=================================================");
            System.out.println("          Selamat Datang Di ATM ALCATRAZ          ");
            System.out.println("=================================================");
            System.out.print("Masukkan username: ");
            String user = br.readLine();
            System.out.print("Masukkan password: ");
            String password = br.readLine();

            if (authenticate(user, password)) {
                System.out.println("\n               Login Berhasil              ");
                System.out.println("\n");

                // Mengambil saldo pengguna dari database
                int saldo = 0;
                int userId = 0;
                try (Connection conn = connect()) {
                    String sql = "SELECT id, balance FROM balances JOIN users ON balances.user_id = users.id WHERE users.username = ?";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, user);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        saldo = rs.getInt("balance");
                        userId = rs.getInt("id");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                while (true) {
                    System.out.println("=================================================");
                    System.out.println("                   ATM ALCATRAZ                 ");
                    System.out.println("=================================================");
                    System.out.println("\n1. Cek Saldo");
                    System.out.println("2. Simpan Uang");
                    System.out.println("3. Ambil Uang");
                    System.out.println("4. History Transaksi Bulanan");
                    System.out.println("5. Kirim Saldo Antar Bank (Bank yang Sama)");
                    System.out.println("6. Kirim Saldo Antar Bank (Bank yang Berbeda)");
                    System.out.println("7. Daftar Pengguna Baru");
                    System.out.println("8. Help");
                    System.out.println("9. Keluar");

                    System.out.print("\nPilih Menu : ");
                    pilihan = Integer.parseInt(br.readLine());
                    switch (pilihan) {
                        case 1:
                            do {
                                System.out.println("=================================================");
                                System.out.println("                    Cek Saldo                    ");
                                System.out.println("       Saldo Anda adalah Rp. " + saldo);
                                System.out.println("=================================================");
                                System.out.println("\n1. Keluar");
                                System.out.println("2. Kembali");
                                System.out.print("\nPilih Menu : ");
                                pilihan2 = Integer.parseInt(br.readLine());
                                if (pilihan2 == 1) {
                                    System.exit(0);
                                }
                            } while (pilihan2 != 2);
                            break;
                        case 2:
                            do {
                                System.out.println("=================================================");
                                System.out.println("   Masukan Jumlah Uang Yang Ingin Anda Simpan    ");
                                System.out.print("     Rp. ");
                                tabung = Integer.parseInt(br.readLine());
                                tambah_saldo[index] = tabung;
                                index++;
                                saldo += tabung;

                                // Update saldo di database
                                try (Connection conn = connect()) {
                                    String sql = "UPDATE balances JOIN users ON balances.user_id = users.id SET balance = ? WHERE users.username = ?";
                                    PreparedStatement pstmt = conn.prepareStatement(sql);
                                    pstmt.setInt(1, saldo);
                                    pstmt.setString(2, user);
                                    pstmt.executeUpdate();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }

                                // Rekam transaksi pemasukan
                                recordTransaction(userId, "deposit", tabung);

                                System.out.println("  Saldo Anda adalah Rp. " + saldo);
                                System.out.println("=================================================");
                                System.out.println("\n1. Keluar");
                                System.out.println("2. Kembali");
                                System.out.print("\nPilih Menu : ");
                                pilihan2 = Integer.parseInt(br.readLine());
                                if (pilihan2 == 1) {
                                    System.exit(0);
                                }
                            } while (pilihan2 != 2);
                            break;
                        case 3:
                            do {
                                System.out.println("=================================================");
                                System.out.println("    Masukan Jumlah Uang Yang Ingin Anda Ambil    ");
                                System.out.print("     Rp. ");
                                ambil = Integer.parseInt(br.readLine());
                                tarik_saldo[index2] = ambil;
                                index2++;
                                isi = saldo;
                                isi -= ambil;
                                if (isi < 5000) {
                                    System.out.println("Saldo Minimal setelah pengambilan harus Rp.5000");
                                } else {
                                    saldo -= ambil;

                                    // Update saldo di database
                                    try (Connection conn = connect()) {
                                        String sql = "UPDATE balances JOIN users ON balances.user_id = users.id SET balance = ? WHERE users.username = ?";
                                        PreparedStatement pstmt = conn.prepareStatement(sql);
                                        pstmt.setInt(1, saldo);
                                        pstmt.setString(2, user);
                                        pstmt.executeUpdate();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }

                                    // Rekam transaksi pengeluaran
                                    recordTransaction(userId, "withdraw", ambil);

                                    System.out.println("Saldo Anda adalah Rp. " + saldo);
                                }
                                System.out.println("=================================================");
                                System.out.println("\n1. Keluar");
                                System.out.println("2. Kembali");
                                System.out.print("\nPilih Menu : ");
                                pilihan2 = Integer.parseInt(br.readLine());
                                if (pilihan2 == 1) {
                                    System.exit(0);
                                }
                            } while (pilihan2 != 2);
                            break;
                        case 4:
                            do {
                                System.out.println("=================================================");
                                System.out.println("                History Transaksi                ");
                                System.out.println("=================================================");
                                try (Connection conn = connect()) {
                                    String sql = "SELECT type, amount, timestamp FROM transaction_history WHERE user_id = ?";
                                    PreparedStatement pstmt = conn.prepareStatement(sql);
                                    pstmt.setInt(1, userId);
                                    ResultSet rs = pstmt.executeQuery();
                                    while (rs.next()) {
                                        String type = rs.getString("type");
                                        int amount = rs.getInt("amount");
                                        Timestamp timestamp = rs.getTimestamp("timestamp");
                                        System.out.println(type + ": Rp. " + amount + " pada " + timestamp);
                                    }
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                                System.out.println("=================================================");
                                System.out.println("\n1. Keluar");
                                System.out.println("2. Kembali");
                                System.out.print("\nPilih Menu : ");
                                pilihan2 = Integer.parseInt(br.readLine());
                                if (pilihan2 == 1) {
                                    System.exit(0);
                                }
                            } while (pilihan2 != 2);
                            break;
                        case 5:
                            do {
                                System.out.println("=================================================");
                                System.out.println("          Kirim Saldo Antar Bank (Bank yang Sama)         ");
                                System.out.println("=================================================");
                                System.out.print("Masukkan jumlah saldo yang ingin dikirim: Rp. ");
                                int kirim = Integer.parseInt(br.readLine());
                                // Misalnya, tidak ada biaya tambahan untuk pengiriman ke bank yang sama
                                if (saldo >= kirim) {
                                    saldo -= kirim;

                                    // Update saldo di database
                                    try (Connection conn = connect()) {
                                        String sql = "UPDATE balances JOIN users ON balances.user_id = users.id SET balance = ? WHERE users.username = ?";
                                        PreparedStatement pstmt = conn.prepareStatement(sql);
                                        pstmt.setInt(1, saldo);
                                        pstmt.setString(2, user);
                                        pstmt.executeUpdate();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }

                                    // Rekam transaksi pengeluaran
                                    recordTransaction(userId, "withdraw", kirim);

                                    System.out.println("Saldo Anda saat ini: Rp. " + saldo);
                                } else {
                                    System.out.println("Saldo Anda tidak mencukupi untuk melakukan pengiriman.");
                                }
                                System.out.println("=================================================");
                                System.out.println("\n1. Keluar");
                                System.out.println("2. Kembali");
                                System.out.print("\nPilih Menu : ");
                                pilihan2 = Integer.parseInt(br.readLine());
                                if (pilihan2 == 1) {
                                    System.exit(0);
                                }
                            } while (pilihan2 != 2);
                            break;
                        case 6:
                            do {
                                System.out.println("=================================================");
                                System.out.println("          Kirim Saldo Antar Bank (Bank yang Berbeda)         ");
                                System.out.println("=================================================");
                                System.out.print("Masukkan jumlah saldo yang ingin dikirim: Rp. ");
                                int kirim = Integer.parseInt(br.readLine());
                                System.out.print("Masukkan kode bank tujuan: ");
                                String kode_bank = br.readLine();

                                // Mengurangi saldo dengan biaya tambahan
                                int totalKirim = kirim + biaya_pengiriman;
                                if (saldo >= totalKirim) {
                                    saldo -= totalKirim;

                                    // Update saldo di database
                                    try (Connection conn = connect()) {
                                        String sql = "UPDATE balances JOIN users ON balances.user_id = users.id SET balance = ? WHERE users.username = ?";
                                        PreparedStatement pstmt = conn.prepareStatement(sql);
                                        pstmt.setInt(1, saldo);
                                        pstmt.setString(2, user);
                                        pstmt.executeUpdate();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }

                                    // Rekam transaksi pengeluaran
                                    recordTransaction(userId, "withdraw", totalKirim);

                                    System.out.println("Saldo Anda saat ini: Rp. " + saldo);
                                } else {
                                    System.out.println("Saldo Anda tidak mencukupi untuk melakukan pengiriman.");
                                }
                                System.out.println("=================================================");
                                System.out.println("\n1. Keluar");
                                System.out.println("2. Kembali");
                                System.out.print("\nPilih Menu : ");
                                pilihan2 = Integer.parseInt(br.readLine());
                                if (pilihan2 == 1) {
                                    System.exit(0);
                                }
                            } while (pilihan2 != 2);
                            break;
                        case 7:
                            do {
                                System.out.println("=================================================");
                                System.out.println("             Daftar Pengguna Baru               ");
                                System.out.println("=================================================");
                                System.out.print("Masukkan username: ");
                                String newUser = br.readLine();
                                System.out.print("Masukkan password: ");
                                String newPassword = br.readLine();
                                if (registerUser(newUser, newPassword)) {
                                    System.out.println("Pendaftaran berhasil!");
                                } else {
                                    System.out.println("Pendaftaran gagal. Username mungkin sudah ada.");
                                }
                                System.out.println("=================================================");
                                System.out.println("\n1. Keluar");
                                System.out.println("2. Kembali");
                                System.out.print("\nPilih Menu : ");
                                pilihan2 = Integer.parseInt(br.readLine());
                                if (pilihan2 == 1) {
                                    System.exit(0);
                                }
                            } while (pilihan2 != 2);
                            break;
                        case 8:
                            System.out.println("Help: Pilih menu sesuai dengan kebutuhan Anda.");
                            break;
                        case 9:
                            System.exit(0);
                            break;
                        default:
                            System.out.println("Pilihan tidak valid.");
                            break;
                    }
                }
            } else {
                System.out.println("Username atau password salah.");
            }
        }
    }

}
