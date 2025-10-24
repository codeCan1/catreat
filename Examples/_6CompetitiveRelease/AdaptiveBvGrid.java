package Examples._6CompetitiveRelease;

//This script simulates adaptive therapy with replacement and blood vessels and tumor measurement error
// Dividing cells (sensitive or resistant) can replace neighboring cells with 0 being no replacement and 1 being replacement at all times
//Resistance is binary phenotype (0 or 1)
//19 June 2019

import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.GridsAndAgents.PDEGrid2D;
import Framework.Gui.GridWindow;
import Framework.Gui.UIGrid;
import Framework.Rand;
import Framework.Tools.FileIO;
import Framework.Util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static Framework.Util.*;

public class AdaptiveBvGrid extends AgentGrid2D<AdaptiveBvCell> {
    //model constants
    public final static int RESISTANT = RGB(1, 0, 0), SENSITIVE = RGB(0, 1, 0);
    public double DIV_PROB_SEN, DIV_PROB_RES, DEATH_PROB, PREV_DRUG, CURRENT_DRUG;
    public int PREV_POP, CURRENT_POP;
    public int SEN_POP, RES_POP;
    public int DRUG_CYCLE_ITERATOR = 0, DRUG_CYCLE_ITERATOR_AT = 0;
    public int AT_START_TICK;
    public double DRUG_ON_TIME = 40, DRUG_OFF_TIME = 160, DRUG_CYCLE_TIME = 200, DRUG_DIFF_RATE = 2.0, DRUG_UPTAKE = 0.91, DRUG_BOUNDARY_VAL = 1.0, DRUG_METABOLISM_TIME = 40.0, DRUG_DEATH=0.2;
    public double DEATH_FUNCTION_CUTOFF = 0.2;
    public double AT_ALPHA, AT_BETA, MAX_TOLERATED_DOSE, MIN_DRUG_DOSE;
    public double REPLACEMENT_THRESHOLD;
    public double MEASUREMENT_NOISE_SD;
    //internal model objects
    public PDEGrid2D drug;
    public Rand rn;
    public int[] divHood = MooreHood(false);
    public boolean HAS_STANDARD_THERAPY_STARTED = false;
    public boolean HAS_AT_STARTED;
    public int CHECK_TUMORSIZE_INTERVAL_AT;
    public int TUMOR_SIZE_TRIGGERING_AT;
    public boolean IS_STANDARD_THERAPY_ON = false;
    public boolean IS_AT_ON;
    public boolean IS_TREATMENT_VACATION_ON;


    public AdaptiveBvGrid(int xDim, int yDim, Rand rn, double birthSensitiveProb, double birthResistantProb, double deathProb, double AT_Alpha, double AT_Beta, double Max_Tolerated_dose, double Min_Drug_Dose, int check_tumorsize_interval_AT, double tumor_size_percent_triggering_AT, double replacement_threshold, double measurement_noise_sd) {
        super(xDim, yDim, AdaptiveBvCell.class);
        this.rn = rn;
        this.DIV_PROB_SEN = birthSensitiveProb;
        this.DIV_PROB_RES = birthResistantProb;
        this.DEATH_PROB = deathProb;
        this.AT_ALPHA = AT_Alpha;
        this.AT_BETA = AT_Beta;
        this.MAX_TOLERATED_DOSE = Max_Tolerated_dose;
        this.MIN_DRUG_DOSE = Min_Drug_Dose;
        this.HAS_AT_STARTED = false;
        this.IS_AT_ON = false;
        this.IS_TREATMENT_VACATION_ON = false;
        this.CHECK_TUMORSIZE_INTERVAL_AT = check_tumorsize_interval_AT;
        this.TUMOR_SIZE_TRIGGERING_AT = (int)(tumor_size_percent_triggering_AT*length);
        this.REPLACEMENT_THRESHOLD = replacement_threshold;
        this.MEASUREMENT_NOISE_SD = measurement_noise_sd;
        drug = new PDEGrid2D(xDim, yDim);
    }

    public static void main(String[] args) {
        int x = 100, y = 100, visScale = 3, tumorRad = 10, msPause = 0;
        double resistantProp = 0.5;
        GridWindow win = new GridWindow("No Therapy, Continuous Therapy, Metronomic Therapy, Adaptive Therapy", x*4, y, visScale,true);
        AdaptiveBvGrid[] models = new AdaptiveBvGrid[4];
        FileIO popsOut=new FileIO("RTBV5.csv","w");
        BufferedReader userInput=new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Please enter in order: 1.Birth Prob Sensitive 2.Birth Prob Resistant 3. Death Prob 4.Alpha Param for AT 5.Beta Param for AT 6.Maximum Tolerated Dose 7. TimeSteps after which tumor size(cell number) is checked for AT 8. AT starts after what fraction of the grid is occupied pressing enter after each");
        double Div_Prob_Sen=0.03;
        double Div_Prob_Res=0.02;
        double deathProb=0.01;
        double AT_alpha=0.5;
        double AT_beta=0.1;
        double Max_Tolerated_Dose=3.0;
        double Min_Drug_Dose=1.0;
        int Check_Tumorsize_Interval_AT=72;
        double Tumor_Size_Percent_Triggering_AT=0.5;
        double Replacement_Threshold=0.95;
        double Measurement_Noise_SD=5;

        for (int i = 0; i < models.length; i++) {
            models[i]=new AdaptiveBvGrid(x,y,new Rand(1),Div_Prob_Sen,Div_Prob_Res,deathProb,AT_alpha,AT_beta,Max_Tolerated_Dose, Min_Drug_Dose,Check_Tumorsize_Interval_AT,Tumor_Size_Percent_Triggering_AT,Replacement_Threshold,Measurement_Noise_SD);
            models[i].InitTumor(tumorRad, resistantProp);
        }
        models[0].DRUG_ON_TIME =0;//no drug
        models[1].DRUG_ON_TIME =models[1].DRUG_CYCLE_TIME;
//        models[2].DRUG_ON_TIME = 40;
//        models[2].DRUG_CYCLE_TIME = 200;
        //constant drug
        //Main run loop
        for (int tick = 0; tick < 10000; tick++) {
            win.TickPause(msPause);
            for (int i = 0; i < models.length; i++) {
                if (i==0||i==1||i==2) {
                    models[i].ModelStep(tick);
                    models[i].DrawModel(win,i);
                } else {
                    models[i].ModelStepAdaptiveTherapy(tick);
                    models[i].DrawModel(win,i);
                }
            }
            //data recording
            popsOut.Write(models[0].Pop()+","+models[0].SEN_POP+","+models[0].RES_POP+","+models[1].Pop()+","+models[1].SEN_POP+","+models[1].RES_POP+","+models[2].Pop()+","+models[2].SEN_POP+","+models[2].RES_POP+","+models[3].Pop()+","+models[3].SEN_POP+","+models[3].RES_POP+","+models[3].CURRENT_DRUG+"\n");
//            if((tick)%100==0) {
            //        win.ToPNG("ModelsTick" +tick+".png");
//            }
        }
        popsOut.Close();
        win.Close();
    }

    public void InitTumor(int radius, double resistantProb) {
        //get a list of indices that fill a circle at the center of the grid
        int[] tumorNeighborhood = CircleHood(true, radius);
        int hoodSize=MapHood(tumorNeighborhood,xDim/2,yDim/2);
        for (int i = 0; i < hoodSize; i++) {

            int tmp = rn.Double() < resistantProb?RESISTANT:SENSITIVE;
            NewAgentSQ(tumorNeighborhood[i]).type = tmp;
            if(tmp==RESISTANT) {
                RES_POP++;
            }
            if(tmp==SENSITIVE) {
                SEN_POP++;
            }
        }


    }

    public void ModelStep(int tick) {

        StandardTherapyChecker(tick);
        if(IS_STANDARD_THERAPY_ON) {

            if(DRUG_CYCLE_ITERATOR<DRUG_ON_TIME) {
                Drug_Deliverer(DRUG_DIFF_RATE,MAX_TOLERATED_DOSE,true);
                DRUG_CYCLE_ITERATOR++;
//                break;
            }

            if(DRUG_CYCLE_ITERATOR>=DRUG_ON_TIME) {

                if (DRUG_CYCLE_ITERATOR<=DRUG_CYCLE_TIME) {
                    Drug_Deliverer(DRUG_DIFF_RATE,MAX_TOLERATED_DOSE,false);
                    DRUG_CYCLE_ITERATOR++;
                }

                if (DRUG_CYCLE_ITERATOR>DRUG_CYCLE_TIME) {
                    Drug_Deliverer(DRUG_DIFF_RATE,MAX_TOLERATED_DOSE,false);
                    DRUG_CYCLE_ITERATOR=0;
                }
            }

        }

        ShuffleAgents(rn);
        for (AdaptiveBvCell cell : this) {
            cell.CellStep();
        }

        //check if drug should enter through the boundaries
//        if (Pop()>TUMOR_SIZE_TRIGGERING_AT) {
//            if (tick > DRUG_START && (tick - DRUG_START) % DRUG_OFF_TIME < DRUG_ON_TIME) {
//                drug.DiffusionADI(DRUG_DIFF_RATE, DRUG_BOUNDARY_VAL);
//            } else {
//                drug.DiffusionADI(DRUG_DIFF_RATE);
//            }
//        }
    }

    public void ModelStepAdaptiveTherapy(int tick) {

        AdaptiveTherapyChecker(tick);

        if(IS_AT_ON) {
            if(DRUG_CYCLE_ITERATOR_AT<=DRUG_ON_TIME) {
                Drug_Deliverer(DRUG_DIFF_RATE,CURRENT_DRUG,true);
                DRUG_CYCLE_ITERATOR_AT++;
            }
            if(DRUG_CYCLE_ITERATOR_AT>DRUG_ON_TIME) {
                Drug_Deliverer(DRUG_DIFF_RATE,CURRENT_DRUG,false);
                DRUG_CYCLE_ITERATOR_AT=0;
                IS_AT_ON=false;
            }
        }

        ShuffleAgents(rn);
        for (AdaptiveBvCell cell : this) {
            cell.CellStep();
        }
    }

    public void Drug_Deliverer(double drug_diffusion_rate, double drug_amount, boolean is_drug_on) {
        if(is_drug_on) {
            double blood_vessel_radius=2;

            int[] bloodVesselNeighborhood1 = CircleHood(true, blood_vessel_radius);
            int bloodVessel1=MapHood(bloodVesselNeighborhood1,25,25);
            for (int i = 0; i<bloodVessel1; i++) {
                drug.Add(bloodVesselNeighborhood1[i],drug_amount/2);
            }

            int[] bloodVesselNeighborhood2 = CircleHood(true, blood_vessel_radius);
            int bloodVessel2=MapHood(bloodVesselNeighborhood2,25,75);
            for (int i = 0; i<bloodVessel2; i++) {
                drug.Add(bloodVesselNeighborhood2[i],drug_amount/2);
            }

            int[] bloodVesselNeighborhood3 = CircleHood(true, blood_vessel_radius);
            int bloodVessel3=MapHood(bloodVesselNeighborhood3,75,25);
            for (int i = 0; i<bloodVessel3; i++) {
                drug.Add(bloodVesselNeighborhood3[i],drug_amount/2);
            }

            int[] bloodVesselNeighborhood4 = CircleHood(true, blood_vessel_radius);
            int bloodVessel4=MapHood(bloodVesselNeighborhood4,75,75);
            for (int i = 0; i<bloodVessel4; i++) {
                drug.Add(bloodVesselNeighborhood4[i],drug_amount/2);
            }

            drug.DiffusionADI(drug_diffusion_rate);
        }
        if (!is_drug_on) {
            drug.DiffusionADI(drug_diffusion_rate);
        }

    }

    public void DrawModel(UIGrid vis, int iModel) {
        for (int i = 0; i < length; i++) {
            AdaptiveBvCell drawMe = GetAgent(i);
            //if the cell does not exist, draw the drug concentration
            vis.SetPix(ItoX(i)+iModel*xDim,ItoY(i), drawMe == null ? HeatMapBRG(drug.Get(i)) : drawMe.type);
        }
    }

    public boolean StandardTherapyChecker(int tick) {
        if (!HAS_STANDARD_THERAPY_STARTED) {
            if(Pop()>=TUMOR_SIZE_TRIGGERING_AT) {
                IS_STANDARD_THERAPY_ON=true;
                return true;
            }
        }

        if(HAS_STANDARD_THERAPY_STARTED) {
            IS_STANDARD_THERAPY_ON=true;
            return true;
        }
        return false;
    }

    public boolean AdaptiveTherapyChecker(int tick) {
//        Start of Adaptive Therapy


        CURRENT_POP = Pop();
        CURRENT_POP = (int)rn.Gaussian(CURRENT_POP,MEASUREMENT_NOISE_SD);
//        System.out.println("Tumor measurement after noise is " +CURRENT_POP);


        if(!HAS_AT_STARTED) {

            if(CURRENT_POP>=TUMOR_SIZE_TRIGGERING_AT) {
                HAS_AT_STARTED=true;
                CURRENT_DRUG=MAX_TOLERATED_DOSE;
                System.out.println("Tick: "+tick+" Current Drug for first dose and Current Pop "+CURRENT_POP+" >= Tumor size triggering AT is "+CURRENT_DRUG+". Prev pop is "+PREV_POP);
                AT_START_TICK=tick;
                PREV_POP=CURRENT_POP;
                PREV_DRUG=CURRENT_DRUG;
                IS_AT_ON=true;
                return true;

            }
        }

        if (HAS_AT_STARTED) {

            if ((tick-AT_START_TICK)%CHECK_TUMORSIZE_INTERVAL_AT==0) {

                if(CURRENT_POP<=(0.5*TUMOR_SIZE_TRIGGERING_AT)) {
                    CURRENT_DRUG=0.0;
                    System.out.println("Tick: "+tick+" Current Drug for CurrentPop "+CURRENT_POP+" <=0.5 of the tumor size triggering AT is "+CURRENT_DRUG+". Prev pop is "+PREV_POP);
                    PREV_POP=CURRENT_POP;
                    PREV_DRUG=(0.5*PREV_DRUG<=MIN_DRUG_DOSE)?MIN_DRUG_DOSE:0.5*PREV_DRUG;
                    IS_AT_ON=true;
                    return true;
                }
                if(CURRENT_POP>(int)(0.95*length)) {
                    CURRENT_DRUG=MAX_TOLERATED_DOSE;
                    System.out.println("Tick: "+tick+" Current Drug for pop "+CURRENT_POP+" >0.95*carrying capacity(xDim*yDim)  "+CURRENT_DRUG+". Prev pop is "+PREV_POP);
                    PREV_POP=CURRENT_POP;
                    PREV_DRUG=CURRENT_DRUG;
                    IS_AT_ON=true;
                    return true;

                }
                if(CURRENT_POP>((int)(((1+AT_BETA)*PREV_POP)))) {

                    CURRENT_DRUG=((1+AT_ALPHA)*PREV_DRUG)>=MAX_TOLERATED_DOSE?MAX_TOLERATED_DOSE:((1+AT_ALPHA)*PREV_DRUG);
                    System.out.println("Tick: "+tick+" Current Drug for CurrentPop "+CURRENT_POP+" >(1+at_beta)*Prev Pop "+PREV_POP+" is "+CURRENT_DRUG);
                    PREV_POP=CURRENT_POP;
                    PREV_DRUG=CURRENT_DRUG;
                    IS_AT_ON=true;
                    return true;
                }

                if(CURRENT_POP<=((int)(((1-AT_BETA)*PREV_POP)))) {

                    CURRENT_DRUG=((1-AT_ALPHA)*PREV_DRUG)<=MIN_DRUG_DOSE?MIN_DRUG_DOSE:(1-AT_ALPHA)*PREV_DRUG;
                    System.out.println("Tick: "+tick+" Current Drug for CurrentPop "+CURRENT_POP+" <=(1-at_beta)*Prev Pop "+PREV_POP+" is "+CURRENT_DRUG);
                    PREV_POP=CURRENT_POP;
                    PREV_DRUG=CURRENT_DRUG;
                    IS_AT_ON=true;
                    return true;
                }

                if(CURRENT_POP>((int)((1-AT_BETA)*PREV_POP)) && CURRENT_POP<=((int)((1+AT_BETA)*PREV_POP))) {
                    CURRENT_DRUG=PREV_DRUG;
                    System.out.println("Tick: "+tick+" Current Drug for Current Pop "+CURRENT_POP+" within the threshold(beta) of prev pop "+PREV_POP+" is "+CURRENT_DRUG);
                    PREV_POP=CURRENT_POP;
                    PREV_DRUG=CURRENT_DRUG;
                    IS_AT_ON=true;
                    return true;
                }

                return false;
            }
        }

        return false;

    }
}

class AdaptiveBvCell extends AgentSQ2Dunstackable<AdaptiveBvGrid> {

//    public double generation_time;
//    public double drug_sensitivity_score;
    public int type;


    public void CellStep() {

        //Consumption of Drug
        G.drug.Mul(Isq(), G.DRUG_UPTAKE);
        //Chance of Death, depends on resistance and drug concentration
        double tmp_random = G.rn.Double();
        if(type==G.SENSITIVE && tmp_random<(G.DEATH_PROB+G.drug.Get(Isq())*G.DRUG_DEATH)) {
            Dispose();
            G.SEN_POP--;
            return;
        }
        if(type==G.RESISTANT && tmp_random<G.DEATH_PROB) {
            Dispose();
            G.RES_POP--;
            return;
        }
        //Chance of Division, depends on resistance
        if (G.rn.Double() < (type == G.RESISTANT ? G.DIV_PROB_RES : G.DIV_PROB_SEN)) {

            int options_empty=MapEmptyHood(G.divHood);
            if(options_empty>0){
                G.NewAgentSQ(G.divHood[G.rn.Int(options_empty)]).type=this.type;
                if(this.type==G.SENSITIVE) {
                    G.SEN_POP++;
                }
                if(this.type==G.RESISTANT) {
                    G.RES_POP++;
                }
            }

            if (options_empty==0 && G.rn.Double()<G.REPLACEMENT_THRESHOLD) { //&& this.type==G.RESISTANT) {

                int options_occupied=MapOccupiedHood(G.divHood);
//                System.out.println("options_occupied is " + options_occupied);
                int tmpAgentIndex=G.divHood[G.rn.Int(options_occupied)];
//                System.out.println("tmpAgentIndex is " + tmpAgentIndex);
                AdaptiveBvCell tmpAgent = G.GetAgent(tmpAgentIndex);
                int tmpAgentType = tmpAgent.type;

                tmpAgent.Dispose();
                if(tmpAgentType==G.SENSITIVE) {
                    G.SEN_POP--;
                }
                if(tmpAgentType==G.RESISTANT) {
                    G.RES_POP--;
                }
//                System.out.println("Agent at "+tmpAgentIndex+"has been disposed");
                G.NewAgentSQ(tmpAgentIndex).type=this.type;
                if(this.type==G.SENSITIVE) {
                    G.SEN_POP++;
                }
                if(this.type==G.RESISTANT) {
                    G.RES_POP++;
                }
//                System.out.println("A new Agent has been created at "+tmpAgentIndex);


            }

        }
    }

}