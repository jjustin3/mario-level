package dk.itu.mario.level.generator;

import java.util.Random;

import dk.itu.mario.MarioInterface.Constraints;
import dk.itu.mario.MarioInterface.GamePlay;
import dk.itu.mario.MarioInterface.LevelGenerator;
import dk.itu.mario.MarioInterface.LevelInterface;
import dk.itu.mario.level.CustomizedLevel;
import dk.itu.mario.level.MyLevel;

public class MyLevelGenerator extends CustomizedLevelGenerator implements LevelGenerator {

	private Random random;

	public LevelInterface generateLevel(GamePlay playerMetrics) {
		LevelInterface level = new MyLevel(320,15,new Random().nextLong(),1,LevelInterface.TYPE_OVERGROUND,playerMetrics);
		try {
			level = this.simulatedAnnealing(playerMetrics, level);
		} catch(CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return level;
	}

	@Override
	public LevelInterface generateLevel(String detailedInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	public LevelInterface simulatedAnnealing(GamePlay playerMetrics, LevelInterface level) throws CloneNotSupportedException {
		MyLevel currLevel = (MyLevel) level;	// original solution
		MyLevel best = currLevel;	// running best solution
		double temperature = 10;
		double coolingRate = 0.97;
		int iterations = 50;
		double iterationRate = 1.02;
		int totalIterations = 150;
		int numIterations = 0;

		if(random == null)
			random = new Random();

		while(numIterations < totalIterations) {
			for(int i = 0; i < iterations; i++) {
				MyLevel newLevel = currLevel.clone(); //check here
				if((newLevel.eval(playerMetrics) < currLevel.eval(playerMetrics)) || (random.nextDouble() < Math.exp((currLevel.eval(playerMetrics)-newLevel.eval(playerMetrics))/temperature))) {
					currLevel = newLevel;
					if(currLevel.eval(playerMetrics) < best.eval(playerMetrics))
						best = currLevel;
				}
			}

			temperature *= coolingRate;
			iterations *= iterationRate;
			numIterations++;
		}
		System.out.println(playerMetrics.coinsCollected);

		return (LevelInterface) best;
	}

}
