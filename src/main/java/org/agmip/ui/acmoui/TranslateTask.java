package org.agmip.ui.acmoui;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.agmip.acmo.translators.AcmoTranslator;
import org.agmip.acmo.translators.apsim.ApsimAcmo;
import org.agmip.acmo.translators.dssat.DssatAcmo;
import org.apache.pivot.util.concurrent.Task;
import org.apache.pivot.util.concurrent.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslateTask extends Task<Boolean> {

	private String model;
	private String inputDirectory;
	private String outputDirectory;
    private static Logger LOG = LoggerFactory.getLogger(TranslateTask.class);

    public TranslateTask(String model, String inputDirectory, String outputDirectory) {
    	this.model = model;
    	this.inputDirectory = inputDirectory;
    	this.outputDirectory = outputDirectory;
    }

    @Override
    public Boolean execute() throws TaskExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(64);
        AcmoTranslator translator;
        try {
        	if (model.equals("DSSAT")) {
                translator = new DssatAcmo();
                LOG.debug("Translating with :" + translator.getClass().getName());
            } else if (model.equals("APSIM")) {
            	translator = new ApsimAcmo();
            } else {
                return false;
            }
            if (translator != null) {
	            Runnable thread = new TranslateRunner(translator, inputDirectory, outputDirectory);
	            executor.execute(thread);

	            // Release executor
	            executor.shutdown();
	            while (!executor.isTerminated()) {
	            }
	            executor = null;
	            return true;
            }
        } catch (Exception ex) {
            throw new TaskExecutionException(ex);
        }
        return true;
    }
}
