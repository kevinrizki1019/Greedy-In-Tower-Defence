package za.co.entelect.challenge;

import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.BuildingType;
import za.co.entelect.challenge.enums.PlayerType;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.Optional;

import static za.co.entelect.challenge.enums.BuildingType.ATTACK;
import static za.co.entelect.challenge.enums.BuildingType.DEFENSE;

public class Bot {
    private static final String NOTHING_COMMAND = "";
    private GameState gameState;
    private GameDetails gameDetails;
    private int gameWidth;
    private int gameHeight;
    private Player myself;
    private Player opponent;
    private List<Building> buildings;
    private List<Missile> missiles;

    /**
     * Constructor
     *
     * @param gameState the game state
     **/
    public Bot(GameState gameState) {
        this.gameState = gameState;
        gameDetails = gameState.getGameDetails();
        gameWidth = gameDetails.mapWidth;
        gameHeight = gameDetails.mapHeight;
        myself = gameState.getPlayers().stream().filter(p -> p.playerType == PlayerType.A).findFirst().get();
        opponent = gameState.getPlayers().stream().filter(p -> p.playerType == PlayerType.B).findFirst().get();

        buildings = gameState.getGameMap().stream()
                .flatMap(c -> c.getBuildings().stream())
                .collect(Collectors.toList());

        missiles = gameState.getGameMap().stream()
                .flatMap(c -> c.getMissiles().stream())
                .collect(Collectors.toList());
    }

    /**
     * Run
     *
     * @return the result
     **/
    public String run() {
        /* Greedy Strategy */
        String command = "";

        // Check the defense stability state and handle it as number one prioritize
        command = doDefense();
        // check the energy sustainability and handle it
        if (needCollectEnergy()) {
            command = placeBuildingRandomlyFromBack(BuildingType.ENERGY);
        }
        // do the attack as aggressive action to create local optimum win
        if (command.equals("")) {
            command = doAttack();
        }

        return command;
    }

    /**
     * Do Defense
     *
     * @return the result
     **/
    private String doDefense() {
        // Place defence building wherever row that have many enemy missiles.
        if (canAffordBuilding(BuildingType.DEFENSE)) {
            int i = 0;
            while (i < gameState.getGameDetails().mapHeight) {
                int enemyMissiles = getAllMissilesForPlayer(PlayerType.B, i).size();
                int myDefenseOnRow = getAllBuildingsForPlayer(PlayerType.A, b->b.buildingType == BuildingType.DEFENSE, i).size();

                if (enemyMissiles >= 4 && myDefenseOnRow == 0) {
                    return placeBuildingInRowFromFront(BuildingType.DEFENSE, i);
                } else {
                    i++;
                }
            }
        }
        return "";
    }

    /**
     * Need collect Energy
     *
     * @return the result
     **/
    private Boolean needCollectEnergy() {
        // Return true if the number of energy buildings is doesn't make energy sustainability state.
        // That is every turn will give < 30 energy. Then, 9 energy building every time in game is necessity to win.
        int myEnergyBuildings = 0;

        for (int i = 0; i < gameState.getGameDetails().mapHeight; i++) {
            myEnergyBuildings += getAllBuildingsForPlayer(PlayerType.A, b->b.buildingType == BuildingType.ENERGY, i).size();
        }

        return myEnergyBuildings < 9;
    }

    /**
     * Do Attack
     *
     * @return the result
     **/
    private String doAttack() {
        // Build attack building in row where it can maximize local win.
        // Then the row where the number of attack row is maximum.
        if (canAffordBuilding(BuildingType.ATTACK)) {
            int optimumRow = -1;
            int maximumAttackInOneRow = -1;
            for (int i = 0; i < gameState.getGameDetails().mapHeight; i++) {
                int myEnergyOnRow = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ENERGY, i).size();
                int myAttackOnRow = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ATTACK, i).size();

                if (myEnergyOnRow <= 1 && myAttackOnRow > maximumAttackInOneRow && myAttackOnRow < 7) {
                    optimumRow = i;
                    maximumAttackInOneRow = myAttackOnRow;
                }
            }
            return placeBuildingInRowFromBack(BuildingType.ATTACK, optimumRow);
        }
        return "";
    }

    /**
     * Get all buildings for player in row y
     *
     * @param playerType the player type
     * @param filter     the filter
     * @param y          the y
     * @return the result
     **/
    private List<Building> getAllBuildingsForPlayer(PlayerType playerType, Predicate<Building> filter, int y) {
        return gameState.getGameMap().stream()
                .filter(c -> c.cellOwner == playerType && c.y == y)
                .flatMap(c -> c.getBuildings().stream())
                .filter(filter)
                .collect(Collectors.toList());
    }

    /**
     * Get all missiles for player in row y
     *
     * @param playerType the player type
     * @param y          the y
     * @return the result
     **/
    private List<Building> getAllMissilesForPlayer(PlayerType playerType, int y) {
        return gameState.getGameMap().stream()
                .filter(c -> c.cellOwner == playerType && c.y == y)
                .flatMap(c -> c.getBuildings().stream())
                .collect(Collectors.toList());
    }

    /**
     * Place building in a random row nearest to the back
     *
     * @param buildingType the building type
     * @return the result
     **/
    private String placeBuildingRandomlyFromBack(BuildingType buildingType) {
        for (int i = 0; i < gameState.getGameDetails().mapWidth / 2; i++) {
            List<CellStateContainer> listOfFreeCells =
                    getListOfEmptyCellsForColumn(i);
            if (!listOfFreeCells.isEmpty()) {
                CellStateContainer pickedCell = listOfFreeCells.get((new Random()).nextInt(listOfFreeCells.size()));
                return buildCommand(pickedCell.x, pickedCell.y, buildingType);
            }
        }
        return "";
    }

    /**
     * Construct build command
     *
     * @param x            the x
     * @param y            the y
     * @param buildingType the building type
     * @return the result
     **/
    private String buildCommand(int x, int y, BuildingType buildingType) {
        return String.format("%s,%d,%s", String.valueOf(x), y, buildingType.getType());
    }

    /**
     * Get all empty cells for column x
     *
     * @param x the x
     * @return the result
     **/
    private List<CellStateContainer> getListOfEmptyCellsForColumn(int x) {
        return gameState.getGameMap().stream()
                .filter(c -> c.x == x && isCellEmpty(x, c.y))
                .collect(Collectors.toList());
    }

    /**
     * Checks if cell at x,y is empty
     *
     * @param x the x
     * @param y the y
     * @return the result
     **/
    private boolean isCellEmpty(int x, int y) {
        Optional<CellStateContainer> cellOptional = gameState.getGameMap().stream()
                .filter(c -> c.x == x && c.y == y)
                .findFirst();

        if (cellOptional.isPresent()) {
            CellStateContainer cell = cellOptional.get();
            return cell.getBuildings().size() <= 0;
        } else {
            System.out.println("Invalid cell selected");
        }
        return true;
    }

    /**
     * Place building in row y nearest to the front
     *
     * @param buildingType the building type
     * @param y            the y
     * @return the result
     **/
    private String placeBuildingInRowFromFront(BuildingType buildingType, int y) {
        for (int i = (gameState.getGameDetails().mapWidth / 2) - 1; i >= 0; i--) {
            if (isCellEmpty(i, y)) {
                return buildCommand(i, y, buildingType);
            }
        }
        return "";
    }

    /**
     * Place building in row y nearest to the back
     *
     * @param buildingType the building type
     * @param y            the y
     * @return the result
     **/
    private String placeBuildingInRowFromBack(BuildingType buildingType, int y) {
        for (int i = 0; i < gameState.getGameDetails().mapWidth / 2; i++) {
            if (isCellEmpty(i, y)) {
                return buildCommand(i, y, buildingType);
            }
        }
        return "";
    }

    /**
     * Can afford building
     *
     * @param buildingType the building type
     * @return the result
     **/
    private boolean canAffordBuilding(BuildingType buildingType) {
        return myself.energy >= gameDetails.buildingsStats.get(buildingType).price;
    }
}

