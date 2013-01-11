package org.agmip.ui.acmoui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.prefs.Preferences;
import org.apache.pivot.beans.Bindable;
import org.apache.pivot.collections.Map;
import org.apache.pivot.util.Filter;
import org.apache.pivot.util.Resources;
import org.apache.pivot.util.concurrent.Task;
import org.apache.pivot.util.concurrent.TaskListener;
import org.apache.pivot.wtk.Action;
import org.apache.pivot.wtk.ActivityIndicator;
import org.apache.pivot.wtk.Alert;
import org.apache.pivot.wtk.BoxPane;
import org.apache.pivot.wtk.Button;
import org.apache.pivot.wtk.Button.State;
import org.apache.pivot.wtk.ButtonPressListener;
import org.apache.pivot.wtk.ButtonStateListener;
import org.apache.pivot.wtk.Checkbox;
import org.apache.pivot.wtk.Component;
import org.apache.pivot.wtk.DesktopApplicationContext;
import org.apache.pivot.wtk.FileBrowserSheet;
import org.apache.pivot.wtk.Label;
import org.apache.pivot.wtk.MessageType;
import org.apache.pivot.wtk.Orientation;
import org.apache.pivot.wtk.PushButton;
import org.apache.pivot.wtk.Sheet;
import org.apache.pivot.wtk.SheetCloseListener;
import org.apache.pivot.wtk.TaskAdapter;
import org.apache.pivot.wtk.TextInput;
import org.apache.pivot.wtk.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AcmoUIWindow extends Window implements Bindable {

    private static Logger LOG = LoggerFactory.getLogger(AcmoUIWindow.class);
    private ActivityIndicator convertIndicator = null;
    private PushButton convertButton = null;
    private PushButton browseToConvert = null;
    private PushButton browseOutputDir = null;
    private Checkbox outputCB = null;
    private Checkbox modelApsim = null;
    private Checkbox modelDssat = null;
    private Label txtStatus = null;
    private Label txtVersion = null;
    private TextInput outputText = null;
    private TextInput convertText = null;
    private ArrayList<Checkbox> checkboxGroup = new ArrayList();
    private ArrayList<String> errors = new ArrayList();
    private Properties versionProperties = new Properties();
    private String acmoVersion = "";
    private Preferences pref = Preferences.userNodeForPackage(getClass());

    public AcmoUIWindow() {
        try {
            InputStream versionFile = getClass().getClassLoader().getResourceAsStream("git.properties");
            versionProperties.load(versionFile);
            versionFile.close();
            StringBuilder qv = new StringBuilder();
            qv.append("Version ");
            qv.append(versionProperties.getProperty("git.commit.id.describe").toString());
            qv.append("(").append(versionProperties.getProperty("git.branch").toString()).append(")");
            acmoVersion = qv.toString();
        } catch (IOException ex) {
            LOG.error("Unable to load version information, version will be blank.");
        }

        Action.getNamedActions().put("fileQuit", new Action() {
            @Override
            public void perform(Component src) {
                DesktopApplicationContext.exit();
            }
        });
    }

    private void validateInputs() {
        errors = new ArrayList();
        boolean anyModelChecked = false;
        for (Checkbox cbox : checkboxGroup) {
            if (cbox.isSelected()) {
                anyModelChecked = true;
            }
        }
        if (!anyModelChecked) {
            errors.add("You need to select an output data source");
        }
        File convertFile = new File(convertText.getText());
        File outputDir = new File(outputText.getText());
        if (!convertFile.exists()) {
            errors.add("You need to select a file to convert");
        }
        if (!outputDir.exists() || !outputDir.isDirectory()) {
            errors.add("You need to select an output directory");
        }
    }

    @Override
    public void initialize(Map<String, Object> ns, URL location, Resources res) {
        convertIndicator = (ActivityIndicator) ns.get("convertIndicator");
        convertButton = (PushButton) ns.get("convertButton");
        browseToConvert = (PushButton) ns.get("browseConvertButton");
        browseOutputDir = (PushButton) ns.get("browseOutputButton");
        txtStatus = (Label) ns.get("txtStatus");
        txtVersion = (Label) ns.get("txtVersion");
        convertText = (TextInput) ns.get("convertText");
        outputText = (TextInput) ns.get("outputText");
        outputCB = (Checkbox) ns.get("outputCB");
        modelApsim = (Checkbox) ns.get("model-apsim");
        modelDssat = (Checkbox) ns.get("model-dssat");

        checkboxGroup.add(modelApsim);
        checkboxGroup.add(modelDssat);

        outputText.setText("");
        txtVersion.setText(acmoVersion);

        convertButton.getButtonPressListeners().add(new ButtonPressListener() {
            @Override
            public void buttonPressed(Button button) {
                validateInputs();
                if (!errors.isEmpty()) {
                    final BoxPane pane = new BoxPane(Orientation.VERTICAL);
                    for (String error : errors) {
                        pane.add(new Label(error));
                    }
                    Alert.alert(MessageType.ERROR, "Cannot Convert", pane, AcmoUIWindow.this);
                    return;
                }
                LOG.info("Starting translation job");
                try {
                    startTranslation();
                } catch (Exception ex) {
                    LOG.error(getStackTrace(ex));
                }
            }
        });

        browseToConvert.getButtonPressListeners().add(new ButtonPressListener() {
            @Override
            public void buttonPressed(Button button) {
                final FileBrowserSheet browse;
                if (convertText.getText().equals("")) {
                    String lastPath = pref.get("last_Input", "");
                    if (lastPath.equals("")) {
                        browse = new FileBrowserSheet(FileBrowserSheet.Mode.OPEN);
                    } else {
                        browse = new FileBrowserSheet(FileBrowserSheet.Mode.OPEN, lastPath);
                    }
                } else {
                    File f = new File(convertText.getText());
                    browse = new FileBrowserSheet(FileBrowserSheet.Mode.OPEN, f.getParent());
                }
                browse.setDisabledFileFilter(new Filter<File>() {
                    @Override
                    public boolean include(File file) {
                        return (file.isFile()
//                                && (!file.getName().toLowerCase().endsWith(".csv"))
//                                && (!file.getName().toLowerCase().endsWith(".agmip"))
//                                && (!file.getName().toLowerCase().endsWith(".json"))
                                && (!file.getName().toLowerCase().endsWith(".zip")));
                    }
                });
                browse.open(AcmoUIWindow.this, new SheetCloseListener() {
                    @Override
                    public void sheetClosed(Sheet sheet) {
                        if (sheet.getResult()) {
                            File convertFile = browse.getSelectedFile();
                            convertText.setText(convertFile.getPath());
                            try {
                                String lastPath = convertFile.getCanonicalFile().getParent();
//                            if (outputText.getText().equals("")) {
                                if (outputCB.getState().equals(State.SELECTED)) {
                                    outputText.setText(lastPath);
                                }
                                pref.put("last_Input", lastPath);
                            } catch (IOException ex) {
                            }
                        }
                    }
                });
            }
        });

        browseOutputDir.getButtonPressListeners().add(new ButtonPressListener() {
            @Override
            public void buttonPressed(Button button) {
                final FileBrowserSheet browse;
                if (outputText.getText().equals("")) {
                    String lastPath = pref.get("last_Output", "");
                    if (lastPath.equals("")) {
                        browse = new FileBrowserSheet(FileBrowserSheet.Mode.SAVE_TO);
                    } else {
                        browse = new FileBrowserSheet(FileBrowserSheet.Mode.SAVE_TO, lastPath);
                    }
                } else {
                    browse = new FileBrowserSheet(FileBrowserSheet.Mode.SAVE_TO, outputText.getText());
                }
                browse.open(AcmoUIWindow.this, new SheetCloseListener() {
                    @Override
                    public void sheetClosed(Sheet sheet) {
                        if (sheet.getResult()) {
                            File outputDir = browse.getSelectedFile();
                            outputText.setText(outputDir.getPath());
                        }
                    }
                });
            }
        });

        outputCB.getButtonStateListeners().add(new ButtonStateListener() {
            @Override
            public void stateChanged(Button button, State state) {
                if (button.getState().equals(State.UNSELECTED)) {
                    browseOutputDir.setEnabled(true);
                } else {
                    browseOutputDir.setEnabled(false);
                }
            }
        });
    }

    private void startTranslation() throws Exception {
        convertIndicator.setActive(true);
        convertButton.setEnabled(false);
        txtStatus.setText("Importing data...");
        TranslateFromTask task = new TranslateFromTask(convertText.getText());
        TaskListener<HashMap> listener = new TaskListener<HashMap>() {
            @Override
            public void taskExecuted(Task<HashMap> t) {
                HashMap data = t.getResult();
                if (!data.containsKey("errors")) {
                    toOutput(data);
                } else {
                    Alert.alert(MessageType.ERROR, (String) data.get("errors"), AcmoUIWindow.this);
                }
            }

            @Override
            public void executeFailed(Task<HashMap> arg0) {
                Alert.alert(MessageType.ERROR, arg0.getFault().getMessage(), AcmoUIWindow.this);
                LOG.error(getStackTrace(arg0.getFault()));
                convertIndicator.setActive(false);
                convertButton.setEnabled(true);
            }
        };
        task.execute(new TaskAdapter(listener));
    }

    private void toOutput(HashMap map) {
        txtStatus.setText("Generating model input files...");
        ArrayList<String> models = new ArrayList();
        if (modelApsim.isSelected()) {
            models.add("APSIM");
        }
        if (modelDssat.isSelected()) {
            models.add("DSSAT");
        }

        TranslateToTask task = new TranslateToTask(models, map, outputText.getText());
        TaskListener<String> listener = new TaskListener<String>() {
            @Override
            public void executeFailed(Task<String> arg0) {
                Alert.alert(MessageType.ERROR, arg0.getFault().getMessage(), AcmoUIWindow.this);
                LOG.error(getStackTrace(arg0.getFault()));
                convertIndicator.setActive(false);
                convertButton.setEnabled(true);
            }

            @Override
            public void taskExecuted(Task<String> arg0) {
                txtStatus.setText("Completed");
                Alert.alert(MessageType.INFO, "Translation completed", AcmoUIWindow.this);
                convertIndicator.setActive(false);
                convertButton.setEnabled(true);
                LOG.info("=== Completed translation job ===");
            }
        };
        task.execute(new TaskAdapter(listener));
    }

    private static String getStackTrace(Throwable aThrowable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }
}
