package Examples._6CompetitiveRelease;

import Framework.GridsAndAgents.AgentGrid2D;
import Framework.GridsAndAgents.AgentSQ2Dunstackable;
import Framework.GridsAndAgents.PDEGrid2D;
import Framework.Gui.GridWindow;
import Framework.Gui.UIGrid;
import Framework.Rand;
import Framework.Tools.FileIO;
import static Framework.Util.*;

public class AdaptiveTherapyGrid extends AgentGrid2D<AdaptiveTherapyCell> {
    //model constants
    public final static int RESISTANT = RGB(0, 1, 0), SENSITIVE = RGB(0, 0, 1);
//    public double DIV_PROB_SEN = 0.025, DIV_PROB_RES = 0.01, DEATH_PROB = 0.001 ; (0.02,0.01,0.01 works well)

    public double DIV_PROB_SEN, DIV_PROB_RES, DEATH_PROB;
    public double DRUG_START = 400, DRUG_PERIOD = 200, DRUG_DURATION = 40, DRUG_DIFF_RATE = 2, DRUG_UPTAKE = 0.91, DRUG_DEATH = 0.2;
    public double AT_ALPHA, AT_BETA;
    public double DRUG_BOUNDARY_VAL;
    public double Pop_Now;
    public double Pop_Before;
    public double Frac_Pop_Change;
    public double Drug_Before;
    public double Drug_Now;
    public double Frac_Drug_Change;

    //internal model objects
    public PDEGrid2D drug;
    public Rand rn;
    public int[] divHood = MooreHood(false);

    public AdaptiveTherapyGrid(int xDim, int yDim, Rand rn, double birthSensitiveProb, double birthResistantProb, double deathProb, double alpha_AT, double beta_AT, double boundary_drug) {
        super(xDim, yDim, AdaptiveTherapyCell.class);
        this.rn = rn;
        this.DIV_PROB_SEN = birthSensitiveProb;
        this.DIV_PROB_RES = birthResistantProb;
        this.DEATH_PROB = deathProb;
        this.AT_ALPHA = alpha_AT;
        this.AT_BETA = beta_AT;
        this.DRUG_BOUNDARY_VAL = boundary_drug;
        drug = new PDEGrid2D(xDim, yDim);
    }

    public static void main(String[] args) {
        int x = 100, y = 100, visScale = 5, tumorRad = 10, msPause = 0;
        double resistantProp = 0.5;
        GridWindow win = new GridWindow("Competitive Release", x * 3, y, visScale, true);
        AdaptiveTherapyGrid[] models = new AdaptiveTherapyGrid[3];
        FileIO popsOut = new FileIO("populations.csv", "w");

        double birthProbSensitive = Double.parseDouble(args[0]);
        double birthProbResistant = Double.parseDouble(args[1]);
        double deathProb = Double.parseDouble(args[2]);
        double AT_Alpha = Double.parseDouble(args[3]);
        double AT_Beta = Double.parseDouble(args[4]);
        double Drug_Boundary_Val = Double.parseDouble(args[5]);


        for (int i = 0; i < models.length; i++) {
            models[i] = new AdaptiveTherapyGrid(x, y, new Rand(1), birthProbSensitive, birthProbResistant, deathProb, AT_Alpha, AT_Beta, Drug_Boundary_Val );
            models[i].InitTumor(tumorRad, resistantProp);
        }
        models[0].DRUG_DURATION = 0;//no drug
        models[1].DRUG_DURATION = models[1].DRUG_PERIOD;//constant drug
        //Main run loop
        System.out.println("Running AT with parameters alpha and beta "+ " "+AT_Alpha+" "+ AT_Beta);
        for (int tick = 0; tick < 10000; tick++) {
            win.TickPause(msPause);

            for (int i = 0; i < models.length; i++) {
                models[i].ModelStep(tick);
                models[i].DrawModel(win, i);
                //data recording
                popsOut.Write(models[0].Pop() + "," + models[1].Pop() + "," + models[2].Pop() + "\n");
                }
            }
            popsOut.Close();
            win.Close();
        }

    public void InitTumor(int radius, double resistantProb) {
        //get a list of indices that fill a circle at the center of the grid
        int[] tumorNeighborhood = CircleHood(true, radius);
        int hoodSize=MapHood(tumorNeighborhood,xDim/2,yDim/2);
        for (int i = 0; i < hoodSize; i++) {
            NewAgentSQ(tumorNeighborhood[i]).type = rn.Double() < resistantProb ? RESISTANT : SENSITIVE;

        this.Pop_Before = this.Pop();
//        double[] drugcheck = drug.GetField();
//        for (double a:drugcheck) {
//            System.out.println(a);
//        }
        }
    }

    public void ModelStep(int tick) {

        ShuffleAgents(rn);

        for (AdaptiveTherapyCell cell : this) {
            cell.CellStep();
        }

        if (tick%100==0) {
            this.Pop_Now = this.Pop();
            double Pop_Change = this.Pop_Now - this.Pop_Before;
            this.Frac_Pop_Change = Pop_Change/this.Pop_Before;
            System.out.println("Pop before, Pop After, Pop Change, and fractional pop change is "+Pop_Before+" "+Pop_Now+" "+Pop_Change+" "+Frac_Pop_Change);
            this.Pop_Before = this.Pop_Now;

            this.Drug_Before = this.DRUG_BOUNDARY_VAL;


//            Adaptive Therapy Protocol
            if (Pop_Now < (0.5)*Pop_Before) {
                this.Drug_Now = 0.0;
                drug.DiffusionADI(DRUG_DIFF_RATE);
                System.out.println("Drug Before, Drug now, Drug change, and Fractional Drug Change is "+ Drug_Before+" "+Drug_Now+" "+(Drug_Now-Drug_Before)+" "+Frac_Drug_Change);

                this.Drug_Before = this.Drug_Now;

            } if(Pop_Now > (1+AT_BETA)*Pop_Before) {
                this.Drug_Now = (1 + AT_ALPHA)*Drug_Before;
                drug.DiffusionADI(DRUG_DIFF_RATE, this.Drug_Now);
                System.out.println("Drug Before, Drug now, Drug change, and Fractional Drug Change is "+ Drug_Before+" "+Drug_Now+" "+(Drug_Now-Drug_Before)+" "+Frac_Drug_Change);

                this.Drug_Before = this.Drug_Now;


            } if(Pop_Now <= (1-AT_BETA)*Pop_Before) {
                this.Drug_Now = (1 - AT_ALPHA)*Drug_Before;
                drug.DiffusionADI(DRUG_DIFF_RATE, Drug_Now);
                System.out.println("Drug Before, Drug now, Drug change, and Fractional Drug Change is "+ Drug_Before+" "+Drug_Now+" "+(Drug_Now-Drug_Before)+" "+Frac_Drug_Change);

                this.Drug_Before = this.Drug_Now;

            } else {
                this.Drug_Now = this.Drug_Before;
                drug.DiffusionADI(DRUG_DIFF_RATE, this.Drug_Now);
                System.out.println("Drug Before, Drug now, Drug change, and Fractional Drug Change is "+ Drug_Before+" "+Drug_Now+" "+(Drug_Now-Drug_Before)+" "+Frac_Drug_Change);

                this.Drug_Before = this.Drug_Now;
            }

            this.Frac_Drug_Change = (Drug_Now-Drug_Before)/Drug_Before;
            //check if drug should enter through the boundaries
//        if (tick > DRUG_START && (tick - DRUG_START) % DRUG_PERIOD < DRUG_DURATION) {
//            drug.DiffusionADI(DRUG_DIFF_RATE, DRUG_BOUNDARY_VAL);
//        } else {
//            drug.DiffusionADI(DRUG_DIFF_RATE);
//        }
        }


    }

    public void DrawModel(UIGrid vis, int iModel) {
        for (int i = 0; i < length; i++) {
            AdaptiveTherapyCell drawMe = GetAgent(i);
            //if the cell does not exist, draw the drug concentration
            vis.SetPix(ItoX(i)+iModel*xDim,ItoY(i), drawMe == null ? HeatMapRGB(drug.Get(i)) : drawMe.type);
        }
    }
}
class AdaptiveTherapyCell extends AgentSQ2Dunstackable<AdaptiveTherapyGrid> {
    public int type;

    public void CellStep() {
        //Consumption of Drug
        G.drug.Mul(Isq(), G.DRUG_UPTAKE);
        //Chance of Death, depends on resistance and drug concentration
        if (G.rn.Double() < G.DEATH_PROB + (type == G.RESISTANT ? 0 : G.drug.Get(Isq()) * G.DRUG_DEATH)) {
            Dispose();
            return;
        }
        //Chance of Division, depends on resistance
        else if (G.rn.Double() < (type == G.RESISTANT ? G.DIV_PROB_RES : G.DIV_PROB_SEN)) {
            int options=MapEmptyHood(G.divHood);
            if(options>0){
                G.NewAgentSQ(G.divHood[G.rn.Int(options)]).type=this.type;
            }
        }
    }
}
