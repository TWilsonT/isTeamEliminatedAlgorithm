/* BaseballElimination.java
   CSC 226 - Summer 2017
   Assignment 4 - Baseball Elimination Program
   
   This template includes some testing code to help verify the implementation.
   To interactively provide test inputs, run the program with
	java BaseballElimination
	
   To conveniently test the algorithm with a large input, create a text file
   containing one or more test divisions (in the format described below) and run
   the program with
	java BaseballElimination file.txt
   where file.txt is replaced by the name of the text file.
   
   The input consists of an integer representing the number of teams in the division and then
   for each team, the team name (no whitespace), number of wins, number of losses, and a list
   of integers represnting the number of games remaining against each team (in order from the first
   team to the last). That is, the text file looks like:
   
	<number of teams in division1>
	<team1_name wins losses games_vs_team1 games_vs_team2 ... games_vs_teamn>
	...
	<teamn_name wins losses games_vs_team1 games_vs_team2 ... games_vs_teamn>
	<number of teams in division2>
	<team1_name wins losses games_vs_team1 games_vs_team2 ... games_vs_teamn>
	...
	<teamn_name wins losses games_vs_team1 games_vs_team2 ... games_vs_teamn>
	...
	
   An input file can contain an unlimited number of divisions but all team names are unique, i.e.
   no team can be in more than one division.


   R. Little - 07/10/2017
*/

import java.util.*;
import java.io.File;
import edu.princeton.cs.algs4.FlowNetwork;
import edu.princeton.cs.algs4.FlowEdge;
import edu.princeton.cs.algs4.FordFulkerson;



//Do not change the name of the BaseballElimination class
public class BaseballElimination{
	// We use an ArrayList to keep track of te eliminated teams.
	public ArrayList<String> eliminated = new ArrayList<String>();

	/* BaseballElimination(s)
		Given an input stream connected to a collection of baseball division
		standings we determine for each division which teams have been eliminated 
		from the playoffs. For each team in each division we create a flow network
		and determine the maxflow in that network. If the maxflow exceeds the number
		of inter-divisional games between all other teams in the division, the current
		team is eliminated.
	*/
	private class TeamData{ 
	    public String teamName;
	    public int wins;
	    public int losses;

	    public TeamData(String team, String winCount, String lossCount){
	    	teamName = team;
	    	wins = Integer.parseInt(winCount);
	    	losses = Integer.parseInt(lossCount);
	    }

	}
	public BaseballElimination(Scanner s){ // for now just look at one team, arbitrarily choose team 5
		// proccess input
		int numTeams = s.nextInt();
		s.nextLine();

		TeamData[] teamData = new TeamData[numTeams];
		int[][] gamesRemaining = new int[numTeams][numTeams]; // holds the remaining game data for all teams
		int teamsProcessed = 0;
		for (int i = 0; i < numTeams && s.hasNextLine(); i++){
			String currentLine = s.nextLine();
			currentLine = currentLine.trim();
			String[] lineArray = currentLine.split("\\s+");

			teamData[i] = new TeamData(lineArray[0], lineArray[1], lineArray[2]);

			for (int j = 0; j < numTeams; j++){
				gamesRemaining[i][j] = Integer.parseInt(lineArray[j + 3]);
			}
		}

		int numTeamsInNetwork = numTeams - 1;

		// create the network (source + matchup vertices + team vertices + sink)
		int numVertices = (numTeamsInNetwork * (numTeamsInNetwork - 1) / 2) + numTeams + 2 + 1;
		for (int teamCheckIndex = 0; teamCheckIndex < numTeams; teamCheckIndex++){
			FlowNetwork network = new FlowNetwork(numVertices);
			int currentVertex = 1; // we will add our vertices based off this number


			// add matchup vertices connecting to the source
			int subtractOneI = 0;
			for (int i = 0; i < numTeams; i++){
				int subtractOneJ = 0;
				if (i == teamCheckIndex) {
					subtractOneI = 1;
					continue;
				}
				for (int j = 0; j < i; j++){
					if (j == teamCheckIndex){
						subtractOneJ = 1;
						continue;
					}
					// capacity for this edge is the number of games between the two teams
					FlowEdge newEdge = new FlowEdge(0, currentVertex + numTeamsInNetwork + 1, gamesRemaining[i][j]);
					network.addEdge(newEdge);

					// connect to the team vertices
					FlowEdge teamOneEdge = new FlowEdge(currentVertex + numTeamsInNetwork + 1, i + 1, Double.POSITIVE_INFINITY);
					FlowEdge teamTwoEdge = new FlowEdge(currentVertex + numTeamsInNetwork + 1, j + 1, Double.POSITIVE_INFINITY);

					network.addEdge(teamOneEdge);
					network.addEdge(teamTwoEdge);

					currentVertex++;
				}

			}

			// calculate how many games remaining team has
			int teamCheckWins = teamData[teamCheckIndex].wins;
			int teamCheckRemainingGames = 0;
			for (int i = 0; i < numTeams; i++){
				teamCheckRemainingGames += gamesRemaining[teamCheckIndex][i];
			}

			// attach team vertices to the sink
			for (int i = 0; i < numTeams; i++){
				if (i == teamCheckIndex){
					continue;
				}
				int capacity = teamCheckRemainingGames + teamCheckWins - teamData[i].wins;
				if (capacity < 0){
					capacity = 0;
					if (!eliminated.contains(teamData[teamCheckIndex].teamName)){
						eliminated.add(teamData[teamCheckIndex].teamName);
					}
				}
				FlowEdge newEdge = new FlowEdge(i+1, currentVertex + numTeams, capacity);
				network.addEdge(newEdge);
			}

			// do ford-fulkerson
			FordFulkerson maxflow = new FordFulkerson(network, 0, currentVertex + numTeams);

			Iterator<FlowEdge> edgeList = network.adj(0).iterator();

			while (edgeList.hasNext()){
				FlowEdge e = edgeList.next();
				if (e.capacity() != e.flow() && e.from() == 0){
					//System.out.printf("%s\n", teamData[teamCheckIndex].teamName);
					if (!eliminated.contains(teamData[teamCheckIndex].teamName)){
						eliminated.add(teamData[teamCheckIndex].teamName);
					}
				}
			}

		}
	}
		
	/* main()
	   Contains code to test the BaseballElimantion function. You may modify the
	   testing code if needed, but nothing in this function will be considered
	   during marking, and the testing process used for marking will not
	   execute any of the code below.
	*/
	public static void main(String[] args){
		Scanner s;
		BaseballElimination be = null;
		if (args.length > 0){
			try{
				s = new Scanner(new File(args[0]));
				be = new BaseballElimination(s);
			} catch(java.io.FileNotFoundException e){
				System.out.printf("Unable to open %s\n",args[0]);
				return;
			}
			System.out.printf("Reading input values %s.\n",args[0]);
		}else{
			s = new Scanner(System.in);
			System.out.printf("Reading input values from stdin.\n");
			be = new BaseballElimination(s);
		}
		
		if (be.eliminated.size() == 0)
			System.out.println("No teams have been eliminated.");
		else
			System.out.println("Teams eliminated: " + be.eliminated);
	}
}
