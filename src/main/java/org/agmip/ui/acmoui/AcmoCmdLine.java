package org.agmip.ui.acmoui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.agmip.acmo.translators.AcmoTranslator;
import org.agmip.acmo.translators.apsim.ApsimAcmo;
import org.agmip.acmo.translators.cropgrownau.CropGrowNAUAcmo;
import org.agmip.acmo.translators.dssat.DssatAcmo;
import static org.agmip.common.Functions.getStackTrace;
import org.agmip.translators.wofost.WofostACMO;
import org.apache.pivot.wtk.Action;
import org.apache.pivot.wtk.Component;
import org.apache.pivot.wtk.DesktopApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Meng Zhang
 */
public class AcmoCmdLine {

    public enum Model {

        DSSAT,
        APSIM,
        WOFOST,
        CropGrowNAU {
            @Override
            public String toString() {
                return "CropGrow-NAU";
            }
        }
    }
    private static Logger LOG = LoggerFactory.getLogger(AcmoCmdLine.class);
    private String outputPath = null;
    private String dataPath = null;
    private Properties versionProperties = new Properties();
    private String acmoVersion = "";
    private Model model = null;
    private boolean helpFlg = false;

    public AcmoCmdLine() {
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

    public void run(String[] args) {

        LOG.info("QuadUI {} lauched with JAVA {} under OS {}", acmoVersion, System.getProperty("java.runtime.version"), System.getProperty("os.name"));
        readCommand(args);
        if (helpFlg) {
            printHelp();
            return;
        } else if (!validate()) {
            LOG.info("Type -h or -help for arguments info");
            return;
        } else {
            argsInfo();
        }

        LOG.info("Starting translation job");
        try {
            startTranslation();
        } catch (Exception ex) {
            LOG.error(getStackTrace(ex));
        }

    }

    private void readCommand(String[] args) {
        int i = 0;
        while (i < args.length && args[i].startsWith("-")) {
            if (args[i].equalsIgnoreCase("-h") || args[i].equalsIgnoreCase("-help")) {
                helpFlg = true;
                return;
            } else if (args[i].equalsIgnoreCase("-dssat")) {
                model = Model.DSSAT;
            } else if (args[i].equalsIgnoreCase("-apsim")) {
                model = Model.APSIM;
            } else if (args[i].equalsIgnoreCase("-wofost")) {
                model = Model.WOFOST;
            } else if (args[i].equalsIgnoreCase("-cropgrownau")) {
                model = Model.CropGrowNAU;
            }
            i++;
        }
        try {
            dataPath = args[i++];
            if (i < args.length) {
                outputPath = args[i];
            } else {
                outputPath = dataPath;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            LOG.error("Path arguments are not enough for selected dome mode");
        }
    }

    private boolean validate() {
        if (model == null) {
            LOG.warn("You need to provide an valid crop model name");
            return false;
        }
        File convertDir = new File(dataPath);
        File outputDir = new File(outputPath);
        if (!convertDir.exists() || !convertDir.isDirectory()) {
            LOG.warn("You need to provide a valid directory to convert");
            return false;
        } else if (convertDir.isDirectory()) {
            try {
                File meta = new File(convertDir.getCanonicalPath() + File.separator + "ACMO_meta.dat");
                if (!meta.exists()) {
                    LOG.warn("You need to inlucde meta data in the directory");
                    return false;
                }
            } catch (IOException e) {
                LOG.warn("You need to provide a valid directory to convert");
                return false;
            }
        }
        if (!outputDir.exists() || !outputDir.isDirectory()) {
            LOG.warn("You need to provide an output directory");
            return false;
        }
        return true;
    }

    private void startTranslation() throws Exception {

        AcmoTranslator translator;
        File output;
        if (model.equals(Model.APSIM)) {
            translator = new ApsimAcmo();
        } else if (model.equals(Model.DSSAT)) {
            translator = new DssatAcmo();
        } else if (model.equals(Model.WOFOST)) {
            translator = new WofostACMO();
        } else if (model.equals(Model.CropGrowNAU)) {
            translator = new CropGrowNAUAcmo();
        } else {
            LOG.error("The crop model {} is not supported by ACMOUI yet", model.toString());
            return;
        }
        LOG.info("Generating ACMO_{}.CSV file...", model.toString());

        try {
            output = translator.execute(dataPath, outputPath);
            if (output == null || !output.exists()) {
                LOG.warn("No file has been generated, please check the input files.");
                LOG.info("=== Cancelled translation job ===");
            } else {
                LOG.info("ACMO report {} is generated.", output.getName());
                LOG.info("=== Completed translation job ===");
            }

        } catch (Exception ex) {
            LOG.error(getStackTrace(ex));
            LOG.info("=== Cancelled translation job ===");
        }
    }

    private void printHelp() {
        System.out.println("\nThe arguments format : <model_option> <data_path> <output_path>");
        System.out.println("\t<model_option>");
        System.out.println("\t\t-D | -dssat\tDSSAT");
        System.out.println("\t\t-A | -apsim\tAPSIM");
        System.out.println("\t\t-W | -wofost\tWOFOST");
        System.out.println("\t\t-C | -cropgrownau\tCropGrow-NAU");
        System.out.println("\t<data_path>");
        System.out.println("\t\tThe path contain the model output file and meta data file generated by QuadUI");
        System.out.println("\t<output_path>");
        System.out.println("\t\tThe path for output.");
        System.out.println("\t\t* If not provided, will use data_path");
        System.out.println("\n");
    }

    private void argsInfo() {
        LOG.info("Crop Model: \t" + model);
        LOG.info("convertPath:\t" + dataPath);
        LOG.info("outputPath:\t" + outputPath);
    }
}
