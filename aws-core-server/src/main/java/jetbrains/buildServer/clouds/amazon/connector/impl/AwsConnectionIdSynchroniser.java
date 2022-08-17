package jetbrains.buildServer.clouds.amazon.connector.impl;

import com.intellij.openapi.diagnostic.Logger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import jetbrains.buildServer.serverSide.CustomDataStorage;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.util.Predicate;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionIdSynchroniser implements Runnable {

  public final static String AWS_CONNECTIONS_INCREMENTAL_ID_STORAGE = "aws.connections.current.incremental.id.storage";
  public final static String AWS_CONNECTIONS_CURRENT_INCREMENTAL_ID_PARAM = "awsConnectionsCurrentId";
  private final static int FIRST_INCREMENTAL_ID = 0;
  private final static Logger LOG = Logger.getInstance(AwsConnectionIdSynchroniser.class.getName());

  private final ProjectManager myProjectManager;
  private final AtomicInteger myCurrentIdentifier;
  private final Predicate<AtomicInteger> myCurrentIdentifierInitialised;

  public AwsConnectionIdSynchroniser(@NotNull final ProjectManager projectManager,
                                     @NotNull AtomicInteger currentIdentifier,
                                     @NotNull Predicate<AtomicInteger> currentIdentifierInitialised) {
    myProjectManager = projectManager;
    myCurrentIdentifier = currentIdentifier;
    myCurrentIdentifierInitialised = currentIdentifierInitialised;
  }

  @Override
  public void run() {
    try {
      CustomDataStorage dataStorage = myProjectManager.getRootProject().getCustomDataStorage(AWS_CONNECTIONS_INCREMENTAL_ID_STORAGE);
      final Map<String, String> values = dataStorage.getValues();

      if (values == null || values.get(AWS_CONNECTIONS_CURRENT_INCREMENTAL_ID_PARAM) == null) {
        setInitialIdentifier(dataStorage);
      } else if (!myCurrentIdentifierInitialised.apply(myCurrentIdentifier)) {
        getIdentifier(values);
      } else {
        syncIdentifier(dataStorage);
      }

    } catch (Exception e) {
      LOG.warnAndDebugDetails("Cannot sync the current AWS identifier", e);
    }
  }

  private void setInitialIdentifier(@NotNull CustomDataStorage dataStorage) {
    dataStorage.putValue(AWS_CONNECTIONS_CURRENT_INCREMENTAL_ID_PARAM, String.valueOf(FIRST_INCREMENTAL_ID));
    dataStorage.flush();
    myCurrentIdentifier.set(FIRST_INCREMENTAL_ID);
  }

  private void getIdentifier(@NotNull final Map<String, String> dataStorageValues) {
    try {
      int currentIdentifierFromDataStorage = Integer.parseInt(dataStorageValues.get(AWS_CONNECTIONS_CURRENT_INCREMENTAL_ID_PARAM));
      myCurrentIdentifier.set(currentIdentifierFromDataStorage);

    } catch (NumberFormatException e) {
      LOG.warnAndDebugDetails("Wrong number in the incremental ID parameter of the CustomDataStorage in the Root Project", e);
    }
  }

  private void syncIdentifier(@NotNull CustomDataStorage dataStorage) {
    dataStorage.updateValues(
      Collections.singletonMap(
        AWS_CONNECTIONS_CURRENT_INCREMENTAL_ID_PARAM,
        String.valueOf(myCurrentIdentifier.get())
      ),
      new HashSet<>()
    );
    dataStorage.flush();
  }
}
