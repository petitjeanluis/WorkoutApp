package Database;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.locks.*;

import javax.imageio.ImageIO;

import com.google.appengine.api.users.User;
import com.googlecode.objectify.ObjectifyService;

public class Storage {
	private static Storage storage;
	private static ArrayList<Exercise> exercises;
	private static ArrayList<Workout> workouts;
	
	//hashmap will be used for locks -- VERY NECESSARY!
	private HashMap<String, ReentrantLock> hash = new HashMap<String,ReentrantLock>();
	
	static {
			ObjectifyService.register(Exercise.class);
			ObjectifyService.register(Workout.class);
			ObjectifyService.register(DataPoint.class);
			ObjectifyService.register(ExerciseData.class);
			ObjectifyService.register(Client.class);
	}
	
	private Storage() {
		exercises = new ArrayList<Exercise>();
		workouts = new ArrayList<Workout>();
	}
	
	//Our Singleton
	public static Storage getInstance() {
		if(storage == null) {
			synchronized (Storage.class) {
				if(storage == null) {
					storage = new Storage();
				}
			}
		}
		return storage;
	}
	
	public void populateExerciseAndWorkout() {
		ExcelParser excelParser = new ExcelParser();
		
		exercises = excelParser.parseExercise();
		workouts = excelParser.parseWorkout(exercises);
	}
	
	public ArrayList<Exercise> getAllExercises() {
		if(workouts.size() == 0 || exercises.size()== 0) {
			populateExerciseAndWorkout();
		}
		
		return exercises;
	}
	
	public ArrayList<Workout> getAllWorkouts() {
		if(workouts.size() == 0 || exercises.size()== 0) {
			populateExerciseAndWorkout();
		}
		
		return workouts;
	}
	
	/*
	 * Load and save methods
	 */
	
	public Client loadClient(User user) {
		//NOT thread safe
		if(workouts.size() == 0 || exercises.size()== 0) {
			populateExerciseAndWorkout();
		}
		
		if(user == null) {
			return null;
		}
		
		Client result = null;
		
		//this first load makes the second one synchronous
		ofy().clear();
		ofy().load().type(Client.class).first().now();
		List<Client> clients = ofy().load().type(Client.class).list();
		
		for(Client c: clients) {
			if(c.getUser().getEmail().equals(user.getEmail())) {
				return c;
			}
		}
		
		//client not found and we are going to create one
		Client newClient = new Client();
		newClient.setUser(user);
		saveClient(newClient);
		
		return newClient;

	}
	
	public Client loadClientSync(User user) {
		//sync is thread safe
		ReentrantLock lock = null;
		
		if(hash.containsKey(user.getEmail())) {
			//lock has already been created for this user
			lock = hash.get(user.getEmail());
		} else {
			lock = new ReentrantLock();
			hash.put(user.getEmail(), lock);
		}
		
		//lock is now held
		lock.lock();
		
		return loadClient(user);
	}
	
	public void saveClient(Client c) {
		//NOT thread safe
		ofy().save().entity(c).now();
		ofy().clear();
	}
	
	public void saveClientSync(User user, Client c) {
		//sync is thread safe
		saveClient(c);
		
		//lock is now free
		hash.get(user.getEmail()).unlock();
	}
			
	/*
	 * lookup Methods
	 */
	
	public Exercise getExerciseFromName(String name) {
		if(workouts.size() == 0 || exercises.size()== 0) {
			populateExerciseAndWorkout();
		}
		
		for(int i = 0; i < exercises.size(); i++) {
			if(exercises.get(i).getName().equals(name)) {
				return exercises.get(i);
			}
		}
		
		//exercise not found
		System.out.println("StorageClass: Exercise not found" + name);
		return null;
	}
	
	public Workout getWorkoutFromName(String name) {
		for(Workout w: workouts) {
			if(w.getWorkoutName().equals(name)) {
				return w;
			}
		}
		return null;
	}
	
	/*
	 * Methods that the social media page uses
	 */
	
	public ArrayList<String> getAllClientsEmails() {
		ofy().clear();
		ofy().load().type(Client.class).first().now();
		List<Client> clients = ofy().load().type(Client.class).list();
		
		ArrayList<String> emailList = new ArrayList<String>();
		for(Client c: clients) {
			if(c.getAllowSharing()) {
				emailList.add(c.getEmail());
			}
		}
		
		return emailList;
	}
	
	public Workout getFriendsWorkoutFromEmail(String email, String friendsWorkout) {
		ofy().clear();
		ofy().load().type(Client.class).first().now();
		List<Client> clients = ofy().load().type(Client.class).list();
		
		for(Client c: clients) {
			if(c.getEmail().equals(email)) {
				for(Workout w: c.getCustomWorkouts()) {
					if(w.getWorkoutName().equals(friendsWorkout)) {
						return new Workout(friendsWorkout, w.getExercises());
					}
				}
			}
		}
		return null;
	}
	
	public ArrayList<String> getFriendsWorkoutNamesFromEmail(String email) {
		ofy().clear();
		ofy().load().type(Client.class).first().now();
		List<Client> clients = ofy().load().type(Client.class).list();
		
		ArrayList<String> result = new ArrayList<String>();
		
		for(Client c: clients) {
			if(c.getEmail().equals(email)) {
				for(Workout w: c.getCustomWorkouts()) {
					result.add(w.getWorkoutName());
				}
			}
		}
		return result;
	}
	
	public Client findFriend(String name) {
		Client result = null;
		
		//this first load makes the second one synchronous
		ofy().load().type(Client.class).first().now();
		List<Client> clients = ofy().load().type(Client.class).list();
		
		for(Client c: clients) {
			if(c.getUser().getEmail().equals(name)) {
				return c;
			}
		}
		
		return result;
	}
	
	/*
	 * Only stuff for objectify below
	 */
	
	//only needed for objectify
	public void saveDataPoint(DataPoint d) {
		ofy().save().entity(d).now();
		ofy().clear();
	}
	
	//only needed for objectify
	public void saveWorkout(Workout w) {
		ofy().save().entity(w).now();
		ofy().clear();
	}
	
	//only needed for objectify
	public void saveExerciseData(ExerciseData e) {
		ofy().save().entity(e).now();
		ofy().clear();
	}
	
	//only needed for objectify
	public void saveExercise(Exercise e) {
		ofy().save().entity(e).now();
		ofy().clear();
	}
}
