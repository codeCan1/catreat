package Examples._8MultiDrugAdaptiveTherapy;

import Framework.Gui.GridWindow;
import Framework.Gui.UIGrid;
import Framework.Gui.UILabel;
import Framework.Gui.UIWindow;
import Framework.Rand;
import Framework.Tools.FileIO;
import Framework.Tools.MultiWellExperiment;

import static Framework.Util.*;

public class MultiwellAdaptiveTherapy{

//    public static void StepModel(CkTandemAddGrid model,int iWell,int iTick){
//        model.ModelStepAdaptiveTherapy(iTick);
//    }
//    public static void StepModel(PpAlternatePenultimateAddGrid model,int iWell,int iTick){
//        model.ModelStepAdaptiveTherapy(iTick);
//    }
    public static void StepModel(PpProgressionAddGrid model,int iWell,int iTick){
        model.ModelStepAdaptiveTherapy(iTick);
    }
//    public static void StepModel(CkIntermittentTherapyGrid model,int iWell,int iTick){
//        model.ModelStepAdaptiveTherapy(iTick);
//    }
//    public static void StepModel(CkTandemDrugHolGrid model,int iWell,int iTick){
//        model.ModelStepAdaptiveTherapy(iTick);
//    }

//    public static int DrawModel(CkTandemAddGrid model,int x,int y){
//        CkTandemAddCell c = model.GetAgent(x,y);
//        return c == null ? HeatMapBRG(model.drugA.Get(x,y)) : c.type;
//    }
//    public static int DrawModel(PpAlternatePenultimateAddGrid model,int x,int y){
//        PpAlternatePenultimateAddCell c = model.GetAgent(x,y);
//        return c == null ? HeatMapBRG(model.drugA.Get(x,y)) : c.type;
//    }
    public static int DrawModel(PpProgressionAddGrid model,int x,int y){
        PpProgressionAddCell c = model.GetAgent(x,y);
        return c == null ? HeatMapBRG(model.drugA.Get(x,y)) : c.type;
    }
//    public static int DrawModel(CkIntermittentTherapyGrid model,int x,int y){
//        CkIntermittentTherapyCell c = model.GetAgent(x,y);
//        return c == null ? HeatMapBRG(model.drugA.Get(x,y)) : c.type;
//    }
//    public static int DrawModel(CkTandemDrugHolGrid model,int x,int y){
//        CkTandemDrugHolCell c = model.GetAgent(x,y);
//        return c == null ? HeatMapBRG(model.drugA.Get(x,y)) : c.type;
//    }

    public static void main(String[] args){
        int x = 100, y = 100, visScale = 2, tumorRad = 10, msPause = 0;
        double resistantProp = 0.5;
        UIGrid vis = new UIGrid( x*4, y, visScale,true);
//        CkTandemAddGrid[] models = new CkTandemAddGrid[4];
        String strategy = "DosAdjAdd50";
        String sub_strategy ="CkTan";
        String file_counter ="xxx1";
//        BufferedReader userInput=new BufferedReader(new InputStreamReader(System.in));
        FileIO popsOut=new FileIO(strategy+"-"+sub_strategy+"-"+file_counter,"w");
        System.out.println("Please enter in order: 1.Birth Prob DOUBLY_SENSITIVE 2.Birth Prob DOUBLY_RESISTANT 3. Death Prob 4.Alpha Param for AT 5.Beta Param for AT 6.Maximum Tolerated Dose 7. TimeSteps after which tumor size(cell number) is checked for AT 8. AT starts after what fraction of the grid is occupied pressing enter after each");
//        double Div_Prob_Sen=0.06;
        double Div_Prob_Sen=0.09;

//        double Div_Prob_Res=0.02;
        double Div_Prob_Res=0.03;

//        double deathProb=0.01;
        double deathProb=0.02;

        double AT_alpha_drug_A=0.5;
        double AT_alpha_drug_B=0.5;
        double AT_gamma_drug_A=0;
        double AT_gamma_drug_B=0;
        double AT_beta=0.1;
//        double Max_Tolerated_Dose_A=3.0;
        double Max_Tolerated_Dose_A=5.0;

//        double Max_Tolerated_Dose_B=3.0;
        double Max_Tolerated_Dose_B=5.0;

        double Min_Drug_Dose_A=0.5;
        double Min_Drug_Dose_B=0.5;
        double MTD_Reduction_Factor_DrugA=0.75;
        double MTD_Reduction_Factor_DrugB=0.75;
        double DRUG_ON_TIME = 1;
        double DRUG_CYCLE_TIME = 24;
        double DRUG_DIFF_RATE = 2.0;
        double DRUG_UPTAKE = 1.0;
        double DRUG_A_DEATH = 0.04;
        double DRUG_B_DEATH = 0.04;

        int Check_Tumorsize_Interval_AT=72;
        double Tumor_Size_Percent_Triggering_AT=0.5;
        double Replacement_Threshold=0.5;
        double Measurement_Noise_SD=5;
        UILabel tickLabel = new UILabel("TICKLABEL                                                                                                 ");
        UILabel popLabel = new UILabel("POPLABEL                                                                                                   ");
        UILabel drugLabel = new UILabel("DRUG                                                                                                                             ");
        UIWindow win = new UIWindow();
        int howManyWells=6;
//        CkTandemAddGrid[] models;
//        PpAlternatePenultimateAddGrid[] models;
        PpProgressionAddGrid[] models;
//        CkIntermittentTherapyGrid[] models;
//        CkTandemDrugHolGrid[] models;


//        models=new CkTandemAddGrid[howManyWells];
//        models=new PpAlternatePenultimateAddGrid[howManyWells];
        models=new PpProgressionAddGrid[howManyWells];
//        models=new CkIntermittentTherapyGrid[howManyWells];
//        models=new CkTandemDrugHolGrid[howManyWells];


//        for (int i=0; i<howManyWells;i++) {
//            models[i]=new CkTandemAddGrid(x,y,new Rand(i+System.currentTimeMillis()),Div_Prob_Sen,Div_Prob_Res,deathProb,AT_alpha_drug_A,AT_alpha_drug_B,AT_gamma_drug_A,AT_gamma_drug_B,AT_beta,Max_Tolerated_Dose_A,Max_Tolerated_Dose_B, Min_Drug_Dose_A,Min_Drug_Dose_B,DRUG_ON_TIME,DRUG_CYCLE_TIME,DRUG_DIFF_RATE,DRUG_UPTAKE,DRUG_A_DEATH,DRUG_B_DEATH,Check_Tumorsize_Interval_AT,Tumor_Size_Percent_Triggering_AT,Replacement_Threshold,Measurement_Noise_SD,tickLabel,popLabel,drugLabel);
//        }
//        for (int i=0; i<howManyWells;i++) {
//            models[i]=new PpAlternatePenultimateAddGrid(x,y,new Rand(i+System.currentTimeMillis()),Div_Prob_Sen,Div_Prob_Res,deathProb,AT_alpha_drug_A,AT_alpha_drug_B,AT_gamma_drug_A,AT_gamma_drug_B,AT_beta,Max_Tolerated_Dose_A,Max_Tolerated_Dose_B, Min_Drug_Dose_A,Min_Drug_Dose_B,DRUG_ON_TIME,DRUG_CYCLE_TIME,DRUG_DIFF_RATE,DRUG_UPTAKE,DRUG_A_DEATH,DRUG_B_DEATH,Check_Tumorsize_Interval_AT,Tumor_Size_Percent_Triggering_AT,Replacement_Threshold,Measurement_Noise_SD,tickLabel,popLabel,drugLabel);
//        }
        for (int i=0; i<howManyWells;i++) {
            models[i]=new PpProgressionAddGrid(x,y,new Rand(i+System.currentTimeMillis()),Div_Prob_Sen,Div_Prob_Res,deathProb,AT_alpha_drug_A,AT_alpha_drug_B,AT_gamma_drug_A,AT_gamma_drug_B,AT_beta,Max_Tolerated_Dose_A,Max_Tolerated_Dose_B, Min_Drug_Dose_A,Min_Drug_Dose_B,DRUG_ON_TIME,DRUG_CYCLE_TIME,DRUG_DIFF_RATE,DRUG_UPTAKE,DRUG_A_DEATH,DRUG_B_DEATH,Check_Tumorsize_Interval_AT,Tumor_Size_Percent_Triggering_AT,Replacement_Threshold,Measurement_Noise_SD,tickLabel,popLabel,drugLabel);
        }
//        for (int i=0; i<howManyWells;i++) {
//            models[i]=new CkIntermittentTherapyGrid(x,y,new Rand(i+System.currentTimeMillis()),Div_Prob_Sen,Div_Prob_Res,deathProb,AT_alpha_drug_A,AT_alpha_drug_B,AT_beta,Max_Tolerated_Dose_A,Max_Tolerated_Dose_B, Min_Drug_Dose_A,Min_Drug_Dose_B,DRUG_ON_TIME,DRUG_CYCLE_TIME,DRUG_DIFF_RATE,DRUG_UPTAKE,DRUG_A_DEATH,DRUG_B_DEATH,Check_Tumorsize_Interval_AT,Tumor_Size_Percent_Triggering_AT,Replacement_Threshold,Measurement_Noise_SD,tickLabel,popLabel,drugLabel);
//        }
//        for (int i=0; i<howManyWells;i++) {
//            models[i]=new CkTandemDrugHolGrid(x,y,new Rand(i+System.currentTimeMillis()),Div_Prob_Sen,Div_Prob_Res,deathProb,AT_alpha_drug_A,AT_alpha_drug_B,AT_beta,Max_Tolerated_Dose_A,Max_Tolerated_Dose_B, Min_Drug_Dose_A,Min_Drug_Dose_B,DRUG_ON_TIME,DRUG_CYCLE_TIME,DRUG_DIFF_RATE,DRUG_UPTAKE,DRUG_A_DEATH,DRUG_B_DEATH,Check_Tumorsize_Interval_AT,Tumor_Size_Percent_Triggering_AT,Replacement_Threshold,Measurement_Noise_SD,tickLabel,popLabel,drugLabel);
//        }
//        CkTandemAddGrid[] models=new CkTandemAddGrid[]{new CkTandemAddGrid(x,y,new Rand(System.currentTimeMillis()),Div_Prob_Sen,Div_Prob_Res,deathProb,AT_alpha_drug_A,AT_alpha_drug_B,AT_gamma_drug_A,AT_gamma_drug_B,AT_beta,Max_Tolerated_Dose_A,Max_Tolerated_Dose_B, Min_Drug_Dose_A,Min_Drug_Dose_B,DRUG_ON_TIME,DRUG_CYCLE_TIME,DRUG_DIFF_RATE,DRUG_UPTAKE,DRUG_A_DEATH,DRUG_B_DEATH,Check_Tumorsize_Interval_AT,Tumor_Size_Percent_Triggering_AT,Replacement_Threshold,Measurement_Noise_SD,tickLabel,popLabel,drugLabel),new CkTandemAddGrid(x,y,new Rand(System.currentTimeMillis()),Div_Prob_Sen,Div_Prob_Res,deathProb,AT_alpha_drug_A,AT_alpha_drug_B,AT_gamma_drug_A,AT_gamma_drug_B,AT_beta,Max_Tolerated_Dose_A,Max_Tolerated_Dose_B, Min_Drug_Dose_A,Min_Drug_Dose_B,DRUG_ON_TIME,DRUG_CYCLE_TIME,DRUG_DIFF_RATE,DRUG_UPTAKE,DRUG_A_DEATH,DRUG_B_DEATH,Check_Tumorsize_Interval_AT,Tumor_Size_Percent_Triggering_AT,Replacement_Threshold,Measurement_Noise_SD,tickLabel,popLabel,drugLabel),new CkTandemAddGrid(x,y,new Rand(System.currentTimeMillis()),Div_Prob_Sen,Div_Prob_Res,deathProb,AT_alpha_drug_A,AT_alpha_drug_B,AT_gamma_drug_A,AT_gamma_drug_B,AT_beta,Max_Tolerated_Dose_A,Max_Tolerated_Dose_B, Min_Drug_Dose_A,Min_Drug_Dose_B,DRUG_ON_TIME,DRUG_CYCLE_TIME,DRUG_DIFF_RATE,DRUG_UPTAKE,DRUG_A_DEATH,DRUG_B_DEATH,Check_Tumorsize_Interval_AT,Tumor_Size_Percent_Triggering_AT,Replacement_Threshold,Measurement_Noise_SD,tickLabel,popLabel,drugLabel),new CkTandemAddGrid(x,y,new Rand(System.currentTimeMillis()),Div_Prob_Sen,Div_Prob_Res,deathProb,AT_alpha_drug_A,AT_alpha_drug_B,AT_gamma_drug_A,AT_gamma_drug_B,AT_beta,Max_Tolerated_Dose_A,Max_Tolerated_Dose_B, Min_Drug_Dose_A,Min_Drug_Dose_B,DRUG_ON_TIME,DRUG_CYCLE_TIME,DRUG_DIFF_RATE,DRUG_UPTAKE,DRUG_A_DEATH,DRUG_B_DEATH,Check_Tumorsize_Interval_AT,Tumor_Size_Percent_Triggering_AT,Replacement_Threshold,Measurement_Noise_SD,tickLabel,popLabel,drugLabel),new CkTandemAddGrid(x,y,new Rand(System.currentTimeMillis()),Div_Prob_Sen,Div_Prob_Res,deathProb,AT_alpha_drug_A,AT_alpha_drug_B,AT_gamma_drug_A,AT_gamma_drug_B,AT_beta,Max_Tolerated_Dose_A,Max_Tolerated_Dose_B, Min_Drug_Dose_A,Min_Drug_Dose_B,DRUG_ON_TIME,DRUG_CYCLE_TIME,DRUG_DIFF_RATE,DRUG_UPTAKE,DRUG_A_DEATH,DRUG_B_DEATH,Check_Tumorsize_Interval_AT,Tumor_Size_Percent_Triggering_AT,Replacement_Threshold,Measurement_Noise_SD,tickLabel,popLabel,drugLabel),new CkTandemAddGrid(x,y,new Rand(System.currentTimeMillis()),Div_Prob_Sen,Div_Prob_Res,deathProb,AT_alpha_drug_A,AT_alpha_drug_B,AT_gamma_drug_A,AT_gamma_drug_B,AT_beta,Max_Tolerated_Dose_A,Max_Tolerated_Dose_B, Min_Drug_Dose_A,Min_Drug_Dose_B,DRUG_ON_TIME,DRUG_CYCLE_TIME,DRUG_DIFF_RATE,DRUG_UPTAKE,DRUG_A_DEATH,DRUG_B_DEATH,Check_Tumorsize_Interval_AT,Tumor_Size_Percent_Triggering_AT,Replacement_Threshold,Measurement_Noise_SD,tickLabel,popLabel,drugLabel),new CkTandemAddGrid(x,y,new Rand(System.currentTimeMillis()),Div_Prob_Sen,Div_Prob_Res,deathProb,AT_alpha_drug_A,AT_alpha_drug_B,AT_gamma_drug_A,AT_gamma_drug_B,AT_beta,Max_Tolerated_Dose_A,Max_Tolerated_Dose_B, Min_Drug_Dose_A,Min_Drug_Dose_B,DRUG_ON_TIME,DRUG_CYCLE_TIME,DRUG_DIFF_RATE,DRUG_UPTAKE,DRUG_A_DEATH,DRUG_B_DEATH,Check_Tumorsize_Interval_AT,Tumor_Size_Percent_Triggering_AT,Replacement_Threshold,Measurement_Noise_SD,tickLabel,popLabel,drugLabel),new CkTandemAddGrid(x,y,new Rand(System.currentTimeMillis()),Div_Prob_Sen,Div_Prob_Res,deathProb,AT_alpha_drug_A,AT_alpha_drug_B,AT_gamma_drug_A,AT_gamma_drug_B,AT_beta,Max_Tolerated_Dose_A,Max_Tolerated_Dose_B, Min_Drug_Dose_A,Min_Drug_Dose_B,DRUG_ON_TIME,DRUG_CYCLE_TIME,DRUG_DIFF_RATE,DRUG_UPTAKE,DRUG_A_DEATH,DRUG_B_DEATH,Check_Tumorsize_Interval_AT,Tumor_Size_Percent_Triggering_AT,Replacement_Threshold,Measurement_Noise_SD,tickLabel,popLabel,drugLabel),new CkTandemAddGrid(x,y,new Rand(System.currentTimeMillis()),Div_Prob_Sen,Div_Prob_Res,deathProb,AT_alpha_drug_A,AT_alpha_drug_B,AT_gamma_drug_A,AT_gamma_drug_B,AT_beta,Max_Tolerated_Dose_A,Max_Tolerated_Dose_B, Min_Drug_Dose_A,Min_Drug_Dose_B,DRUG_ON_TIME,DRUG_CYCLE_TIME,DRUG_DIFF_RATE,DRUG_UPTAKE,DRUG_A_DEATH,DRUG_B_DEATH,Check_Tumorsize_Interval_AT,Tumor_Size_Percent_Triggering_AT,Replacement_Threshold,Measurement_Noise_SD,tickLabel,popLabel,drugLabel)};
//        for (CkTandemAddGrid model : models) {
//            model.InitTumor(tumorRad, resistantProp);
//        }
//        for (PpAlternatePenultimateAddGrid model : models) {
//            model.InitTumor(tumorRad, resistantProp);
//        }
        for (PpProgressionAddGrid model : models) {
            model.InitTumor(tumorRad, resistantProp);
        }
//        for (CkIntermittentTherapyGrid model : models) {
//            model.InitTumor(tumorRad, resistantProp);
//        }
//        for (CkTandemDrugHolGrid model : models) {
//            model.InitTumor(tumorRad, resistantProp);
//        }
        
//        MultiWellExperiment<CkTandemAddGrid> expt=new MultiWellExperiment<CkTandemAddGrid>(3,2,models,x,y,WHITE,3,MultiwellAdaptiveTherapy::StepModel,MultiwellAdaptiveTherapy::DrawModel);
//        MultiWellExperiment<PpAlternatePenultimateAddGrid> expt=new MultiWellExperiment<PpAlternatePenultimateAddGrid>(3,2,models,x,y,WHITE,3,MultiwellAdaptiveTherapy::StepModel,MultiwellAdaptiveTherapy::DrawModel);
        MultiWellExperiment<PpProgressionAddGrid> expt=new MultiWellExperiment<PpProgressionAddGrid>(3,2,models,x,y,WHITE,3,MultiwellAdaptiveTherapy::StepModel,MultiwellAdaptiveTherapy::DrawModel);
//        MultiWellExperiment<CkIntermittentTherapyGrid> expt=new MultiWellExperiment<CkIntermittentTherapyGrid>(3,2,models,x,y,WHITE,3,MultiwellAdaptiveTherapy::StepModel,MultiwellAdaptiveTherapy::DrawModel);
//        MultiWellExperiment<CkTandemDrugHolGrid> expt=new MultiWellExperiment<CkTandemDrugHolGrid>(3,2,models,x,y,WHITE,3,MultiwellAdaptiveTherapy::StepModel,MultiwellAdaptiveTherapy::DrawModel);


        //USE THE S KEY TO SAVE THE STATE, AND THE L KEY TO LOAD THE STATE
//        expt.Run(5000,false,0);
        expt.RunGIF(5000,"PpProgHighTurnover.gif",10,false);
    }
}
