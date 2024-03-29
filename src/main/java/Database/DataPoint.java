package Database;

import java.util.Date;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
public class DataPoint {
	@Id Long id;
	
	@Index int weight;
	@Index int reps;
	@Index int sets;
	@Index Date date;
	
	public DataPoint() {
		//need this no-arg constructor for objectify
	}
	
	public DataPoint(int weight, int reps, Date date) {
		this.weight = weight;
		this.reps = reps;
		this.date = date;
		Storage.getInstance().saveDataPoint(this);
	}
	
	public DataPoint(int weight, int reps, int sets, Date date) {
		this.weight = weight;
		this.reps = reps;
		this.date = date;
		this.sets = sets;
		Storage.getInstance().saveDataPoint(this);
	}
	
	//constructor just for tests
	public DataPoint(int weight, int reps, int sets, Date date, boolean fake) {	
		this.weight = weight;
		this.reps = reps;
		this.date = date;
	}
	
	public int getWeight() {
		return weight;
	}

	public int getReps() {
		return reps;
	}

	public Date getDate() {
		return date;
	}
	
	public int getSets() {
		return sets;
	}
	
	public void setSet(int num) {
		sets = num;
	}
	
	public void resetSet() {
		sets = 1;
	}
	
	public void updateDataPoint(DataPoint p) {
		//does NOT update sets
		date = p.getDate();
		reps = p.getReps();
		weight = p.getWeight();
	}
}
