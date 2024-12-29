// Import required Burp Suite API classes for extension development
package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

// Import Java Swing components for GUI
import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

// Main extension class that implements BurpExtension interface for initialization
// and HttpHandler for processing HTTP traffic
public class BurpExtender implements BurpExtension, HttpHandler {
    // Class-level variables to maintain state and store references
    private MontoyaApi api;                // Reference to Burp Suite's API
    private Set<String> harvestedUrls;     // Set to store unique URLs
    private JFrame frame;                  // Main window of the extension
    private JTextArea urlTextArea;         // Text area to display harvested URLs
    private JLabel statusLabel;            // Label to show count of harvested URLs

    // Initialize method called by Burp Suite when loading the extension
    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        this.harvestedUrls = new HashSet<>();  // Initialize empty set for URLs

        // Set the extension name in Burp Suite
        api.extension().setName("URL Harvester");

        // Register this class as an HTTP handler to process traffic
        api.http().registerHttpHandler(this);

        // Create and show the GUI
        createUI();
        frame.setVisible(true);
    }

    // Method to create the graphical user interface
    private void createUI() {
        // Create main window
        frame = new JFrame("URL Harvester");
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        // Create and add status label at the top
        statusLabel = new JLabel("URLs Harvested: 0");
        frame.add(statusLabel, BorderLayout.NORTH);

        // Create text area for displaying URLs with scroll functionality
        urlTextArea = new JTextArea();
        urlTextArea.setEditable(false);  // Make it read-only
        JScrollPane scrollPane = new JScrollPane(urlTextArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Create panel for buttons at the bottom
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save to File");
        JButton clearButton = new JButton("Clear URLs");
        JButton refreshButton = new JButton("Refresh Display");

        // Add action listeners to buttons
        saveButton.addActionListener(e -> saveToFile());         // Save URLs to file
        clearButton.addActionListener(e -> {                     // Clear all harvested URLs
            harvestedUrls.clear();
            updateUrlDisplay();
        });
        refreshButton.addActionListener(e -> updateUrlDisplay());// Refresh the display

        // Add buttons to panel
        buttonPanel.add(saveButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(refreshButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        // Set window close behavior
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    // Method called for each HTTP request intercepted by Burp Suite
    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        // Check if request is from Proxy or Target tools
        if (requestToBeSent.toolSource().isFromTool(ToolType.PROXY, ToolType.TARGET)) {
            String url = requestToBeSent.url();

            // Add URL if it's in scope and not already harvested
            if (api.scope().isInScope(url) && !harvestedUrls.contains(url)) {
                harvestedUrls.add(url);
                api.logging().logToOutput("Harvested new URL: " + url);
                // Update UI on the Event Dispatch Thread
                SwingUtilities.invokeLater(this::updateUrlDisplay);
            }
        }
        // Continue with the request without modification
        return RequestToBeSentAction.continueWith(requestToBeSent);
    }

    // Method called for each HTTP response received by Burp Suite
    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        // Continue with the response without modification
        return ResponseReceivedAction.continueWith(responseReceived);
    }

    // Method to update the URL display in the GUI
    private void updateUrlDisplay() {
        // Build string of all URLs
        StringBuilder sb = new StringBuilder();
        for (String url : harvestedUrls) {
            sb.append(url).append("\n");
        }
        // Update text area and status label
        urlTextArea.setText(sb.toString());
        statusLabel.setText("URLs Harvested: " + harvestedUrls.size());
    }

    // Method to save harvested URLs to a file
    private void saveToFile() {
        // Show file chooser dialog
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            try (FileWriter writer = new FileWriter(fileChooser.getSelectedFile())) {
                // Write each URL to file
                for (String url : harvestedUrls) {
                    writer.write(url + "\n");
                }
                // Show success message
                JOptionPane.showMessageDialog(frame, "URLs saved successfully!");
            } catch (IOException e) {
                // Show error message if save fails
                JOptionPane.showMessageDialog(frame,
                        "Error saving file: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
