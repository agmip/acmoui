package org.agmip.ui.acmoui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import java.util.prefs.Preferences;
import org.agmip.acmo.translators.AcmoTranslator;
import org.agmip.acmo.translators.apsim.ApsimAcmo;
import org.agmip.acmo.translators.cropgrownau.CropGrowNAUAcmo;
import org.agmip.acmo.translators.dssat.DssatAcmo;
import org.agmip.common.Functions;
import org.agmip.translators.wofost.WofostACMO;
import org.apache.pivot.beans.Bindable;
import org.apache.pivot.collections.Map;
import org.apache.pivot.util.Filter;
import org.apache.pivot.util.Resources;
import org.apache.pivot.wtk.Action;
import org.apache.pivot.wtk.ActivityIndicator;
import org.apache.pivot.wtk.Alert;
import org.apache.pivot.wtk.BoxPane;
import org.apache.pivot.wtk.Button;
import org.apache.pivot.wtk.Button.State;
import org.apache.pivot.wtk.ButtonGroup;
import org.apache.pivot.wtk.ButtonGroupListener;
import org.apache.pivot.wtk.ButtonPressListener;
import org.apache.pivot.wtk.ButtonStateListener;
import org.apache.pivot.wtk.Checkbox;
import org.apache.pivot.wtk.Component;
import org.apache.pivot.wtk.DesktopApplicationContext;
import org.apache.pivot.wtk.FileBrowserSheet;
import org.apache.pivot.wtk.Label;
import org.apache.pivot.wtk.LinkButton;
import org.apache.pivot.wtk.MessageType;
import org.apache.pivot.wtk.Orientation;
import org.apache.pivot.wtk.PushButton;
import org.apache.pivot.wtk.RadioButton;
import org.apache.pivot.wtk.Sheet;
import org.apache.pivot.wtk.SheetCloseListener;
import org.apache.pivot.wtk.TextInput;
import org.apache.pivot.wtk.Window;
import org.apache.pivot.wtk.content.ButtonData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AcmoUIWindow extends Window implements Bindable {

    private static Logger LOG = LoggerFactory.getLogger(AcmoUIWindow.class);
    private ActivityIndicator convertIndicator = null;
    private PushButton convertButton = null;
    private PushButton browseToConvert = null;
    private PushButton browseOutputDir = null;
    private Checkbox outputCB = null;
    private ButtonGroup modelBtnGrp = null;
    private RadioButton modelApsim = null;
    private RadioButton modelDssat = null;
    private RadioButton modelWofost = null;
    private RadioButton modelCgnau = null;
    private Boolean FirstSelect = true;
    private Label txtStatus = null;
    private Label txtVersion = null;
    private TextInput outputText = null;
    private TextInput convertText = null;
    private LinkButton outputLB = null;
//    private ArrayList<RadioButton> radioBtnGroup = new ArrayList();
    private ArrayList<String> errors = new ArrayList();
    private Properties versionProperties = new Properties();
    private String acmoVersion = "";
    private Preferences pref = Preferences.userNodeForPackage(getClass());
    private ButtonPressListener outputLinkLsn = null;
    private String model = "";

    public AcmoUIWindow() {
        try {
            InputStream versionFile = getClass().getClassLoader().getResourceAsStream("product.properties");
            versionProperties.load(versionFile);
            versionFile.close();
            StringBuilder qv = new StringBuilder();
            String buildType = versionProperties.getProperty("product.buildtype").toString();
            qv.append("Version ");
            qv.append(versionProperties.getProperty("product.version").toString());
            qv.append("-").append(versionProperties.getProperty("product.buildversion").toString());
            qv.append("(").append(buildType).append(")");
            if (buildType.equals("dev")) {
                qv.append(" [").append(versionProperties.getProperty("product.buildts")).append("]");
            }
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
        boolean anyModelChecked = modelBtnGrp.getSelection() != null;
//        for (RadioButton cbox : radioBtnGroup) {
//            if (cbox.isSelected()) {
//                anyModelChecked = true;
//                break;
//            }
//        }
        if (!anyModelChecked) {
            errors.add("You need to select an output data source");
        }
        File convertDir = new File(convertText.getText());
        File outputDir = new File(outputText.getText());
        if (!convertDir.exists() || !convertDir.isDirectory()) {
            errors.add("You need to select a directory to convert");
        } else if (convertDir.isDirectory()) {
            try {
                File meta = new File(convertDir.getCanonicalPath() + File.separator + "ACMO_meta.dat");
                if (!meta.exists()) {
                    errors.add("You need to inlucde meta data in the directory");
                }
            } catch (IOException e) {
                errors.add("You need to select a valid directory to convert");
            }
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
        modelBtnGrp = (ButtonGroup) ns.get("models");
        modelApsim = (RadioButton) ns.get("model-apsim");
        modelDssat = (RadioButton) ns.get("model-dssat");
        modelWofost = (RadioButton) ns.get("model-wofost");
        modelCgnau = (RadioButton) ns.get("model-cgnau");
        outputLB = (LinkButton) ns.get("outputLB");

//        radioBtnGroup.add(modelApsim);
//        radioBtnGroup.add(modelDssat);
        outputText.setText("");
        txtVersion.setText(acmoVersion);
        LOG.info("ACMOUI {} lauched with JAVA {} under OS {}", acmoVersion, System.getProperty("java.runtime.version"), System.getProperty("os.name"));

        modelBtnGrp.getButtonGroupListeners().add(new ButtonGroupListener() {
            @Override
            public void buttonAdded(ButtonGroup bg, Button button) {
            }

            @Override
            public void buttonRemoved(ButtonGroup bg, Button button) {
            }

            @Override
            public void selectionChanged(ButtonGroup bg, Button button) {
                if (!FirstSelect) {
                    convertText.setText("");
                    outputText.setText("");
                    txtStatus.setText("");
                } else {
                    FirstSelect = false;
                }
            }
        });

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
                    LOG.error(Functions.getStackTrace(ex));
                }
            }
        });

        browseToConvert.getButtonPressListeners().add(new ButtonPressListener() {
            @Override
            public void buttonPressed(Button button) {
                final FileBrowserSheet browse;
                if (convertText.getText().equals("")) {
                    String lastPath = pref.get("last_Input", "");
                    File tmp = new File(lastPath);
                    if (lastPath.equals("") || !tmp.exists()) {
                        browse = new FileBrowserSheet(FileBrowserSheet.Mode.SAVE_TO);
                    } else {
                        if (!tmp.isDirectory()) {
                            lastPath = tmp.getParentFile().getPath();
                        }
                        File f = new File(lastPath);
                        browse = new FileBrowserSheet(FileBrowserSheet.Mode.SAVE_TO, f.getParent());
                    }
                } else {
                    File f = new File(convertText.getText());
                    browse = new FileBrowserSheet(FileBrowserSheet.Mode.SAVE_TO, f.getParent());
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
//                            if (outputText.getText().equals("")) {
                            if (outputCB.getState().equals(State.SELECTED)) {
                                outputText.setText(convertFile.getPath());
                                pref.put("last_Input", convertFile.getPath());
                            }
                            txtStatus.setText("");
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
                    if (lastPath.equals("") || !new File(lastPath).exists()) {
                        browse = new FileBrowserSheet(FileBrowserSheet.Mode.SAVE_TO);
                    } else {
                        File f = new File(lastPath);
                        browse = new FileBrowserSheet(FileBrowserSheet.Mode.SAVE_TO, f.getParent());
                    }
                } else {
                    File f = new File(outputText.getText());
                    browse = new FileBrowserSheet(FileBrowserSheet.Mode.SAVE_TO, f.getParent());
                }
                browse.open(AcmoUIWindow.this, new SheetCloseListener() {
                    @Override
                    public void sheetClosed(Sheet sheet) {
                        if (sheet.getResult()) {
                            File outputDir = browse.getSelectedFile();
                            outputText.setText(outputDir.getPath());
                            pref.put("last_Output", outputDir.getPath());
                            txtStatus.setText("");
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
                    outputText.setText(convertText.getText());
                }
            }
        });
    }

    private void startTranslation() throws Exception {

        AcmoTranslator translator;
        File output;
        if (modelApsim.isSelected()) {
            model = "APSIM";
            translator = new ApsimAcmo();
        } else if (modelDssat.isSelected()) {
            model = "DSSAT";
            translator = new DssatAcmo();
        } else if (modelWofost.isSelected()) {
            model = "WOFOST";
            translator = new WofostACMO();
        } else if (modelCgnau.isSelected()) {
            model = "CropGrow-NAU";
            translator = new CropGrowNAUAcmo();
        } else {
            Alert.alert(MessageType.ERROR, "You need to select an output data source", AcmoUIWindow.this);
            return;
        }

        convertIndicator.setActive(true);
        convertButton.setEnabled(false);
        txtStatus.setText("Generating ACMO_" + model + ".CSV file...");
        outputLB.setVisible(false);
        if (outputLinkLsn != null) {
            outputLB.getButtonPressListeners().remove(outputLinkLsn);
            outputLinkLsn = null;
        }

        try {
            output = translator.execute(convertText.getText(), outputText.getText());
            if (output == null || !output.exists()) {
                txtStatus.setText("Cancelled");
                Alert.alert(MessageType.ERROR, "No file has been generated, please check the input file", AcmoUIWindow.this);
                LOG.info("No file has been generated.");
                LOG.info("=== Cancelled translation job ===");
            } else {
                final String file = output.getCanonicalPath();
                txtStatus.setText("Completed");
                Alert.alert(MessageType.INFO, "Translation completed", AcmoUIWindow.this);
                outputLB.setVisible(true);
                outputLB.setButtonData(new ButtonData(output.getName()));
                outputLinkLsn = new ButtonPressListener() {
                    @Override
                    public void buttonPressed(Button button) {
                        try {
                            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "\"\"", file);
                            pb.start();
                        } catch (IOException winEx) {
                            try {
                                ProcessBuilder pb = new ProcessBuilder("open", file);
                                pb.start();
                            } catch (IOException macEx) {
                                Alert.alert(MessageType.ERROR, "Your OS can not open the file by using this link", AcmoUIWindow.this);
                                LOG.error(Functions.getStackTrace(winEx));
                                LOG.error(Functions.getStackTrace(macEx));
                            }
                        }
                    }
                };
                outputLB.getButtonPressListeners().add(outputLinkLsn);
                LOG.info("=== Completed translation job ===");
            }

        } catch (Exception ex) {
            txtStatus.setText("Failed");
            LOG.error(Functions.getStackTrace(ex));
            Alert.alert(MessageType.ERROR, ex.getMessage(), AcmoUIWindow.this);
            LOG.info("=== Cancelled translation job ===");
        }

        convertIndicator.setActive(false);
        convertButton.setEnabled(true);

//        TaskListener<HashMap> listener = new TaskListener<HashMap>() {
//            @Override
//            public void taskExecuted(Task<HashMap> t) {
//                HashMap data = t.getResult();
//                if (!data.containsKey("errors")) {
//                    toOutput(data);
//                } else {
//                    Alert.alert(MessageType.ERROR, (String) data.get("errors"), AcmoUIWindow.this);
//                }
//            }
//
//            @Override
//            public void executeFailed(Task<HashMap> arg0) {
//                Alert.alert(MessageType.ERROR, arg0.getFault().getMessage(), AcmoUIWindow.this);
//                LOG.error(getStackTrace(arg0.getFault()));
//                convertIndicator.setActive(false);
//                convertButton.setEnabled(true);
//            }
//        };
//        try {
//            TranslateFromTask task = new TranslateFromTask(model, convertText.getText());
//            task.execute(new TaskAdapter(listener));
//        } catch (Exception ex) {
//            convertIndicator.setActive(false);
//            convertButton.setEnabled(true);
//            txtStatus.setText("Failed");
//            if (ex.getMessage().contains("Meta data is missing")) {
//                Alert.alert(MessageType.ERROR, "Meta data must be included in the selected directory", AcmoUIWindow.this);
//            } else {
//                Alert.alert(MessageType.ERROR, ex.getMessage(), AcmoUIWindow.this);
//                throw ex;
//            }
//        }
    }
//    private void toOutput(HashMap map) {
//        txtStatus.setText("Generating ACMO.CSV file...");
//        TranslateToTask task = new TranslateToTask(model, map, outputText.getText());
//        TaskListener<String> listener = new TaskListener<String>() {
//            @Override
//            public void executeFailed(Task<String> arg0) {
//                Alert.alert(MessageType.ERROR, arg0.getFault().getMessage(), AcmoUIWindow.this);
//                LOG.error(getStackTrace(arg0.getFault()));
//                convertIndicator.setActive(false);
//                convertButton.setEnabled(true);
//            }
//
//            @Override
//            public void taskExecuted(Task<String> arg0) {
//                convertIndicator.setActive(false);
//                convertButton.setEnabled(true);
//                final String file = arg0.getResult();
//                if (!file.equals("")) {
//                    txtStatus.setText("Completed");
//                    Alert.alert(MessageType.INFO, "Translation completed", AcmoUIWindow.this);
//                    outputLB.setVisible(true);
//                    outputLB.setButtonData(new ButtonData(new File(file).getName()));
//                    outputLinkLsn = new ButtonPressListener() {
//                        @Override
//                        public void buttonPressed(Button button) {
//                            try {
//                                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "\"\"", file);
//                                pb.start();
//                            } catch (IOException ex) {
//                                try {
//                                    ProcessBuilder pb = new ProcessBuilder("open", file);
//                                    pb.start();
//                                } catch (IOException ex1) {
//                                    Alert.alert(MessageType.ERROR, "Your OS can not open the file by using this link", AcmoUIWindow.this);
//                                    LOG.error(getStackTrace(ex));
//                                }
//                            }
//                        }
//                    };
//                    outputLB.getButtonPressListeners().add(outputLinkLsn);
//                } else {
//                    txtStatus.setText("Cancelled");
//                    Alert.alert(MessageType.ERROR, "No file has been generated, please check the input file", AcmoUIWindow.this);
//                    LOG.info("No file has been generated.");
//                }
//                LOG.info("=== Cancelled translation job ===");
//            }
//        };
//        task.execute(new TaskAdapter(listener));
//    }
}
