@Component
public class StartupDbCheck {

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void logConnectionInfo() {
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("✅ Connected as: " + conn.getMetaData().getUserName());
            System.out.println("✅ DB URL: " + conn.getMetaData().getURL());
        } catch (SQLException e) {
            System.err.println("❌ Connection failed: " + e.getMessage());
        }
    }
}
