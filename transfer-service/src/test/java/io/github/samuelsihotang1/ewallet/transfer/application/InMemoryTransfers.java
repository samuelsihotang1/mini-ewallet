package io.github.samuelsihotang1.ewallet.transfer.application;

import io.github.samuelsihotang1.ewallet.events.TransferEvent;
import io.github.samuelsihotang1.ewallet.transfer.domain.Transfer;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferId;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Fakes of the storage, event and wallet-directory ports, for tests without infrastructure. */
class InMemoryTransfers implements TransferRepository, TransferEvents, WalletDirectory {

  final Map<TransferId, Transfer> transfers = new HashMap<>();
  final List<TransferEvent> published = new ArrayList<>();
  final Map<UUID, String> owners = new HashMap<>();
  int directoryCalls;
  boolean directoryDown;

  @Override
  public Optional<Transfer> findById(TransferId id) {
    return Optional.ofNullable(transfers.get(id));
  }

  @Override
  public Optional<Transfer> lockById(TransferId id) {
    return findById(id);
  }

  @Override
  public Optional<Transfer> findByIdempotencyKey(UUID sourceWalletId, String idempotencyKey) {
    return transfers.values().stream()
        .filter(t -> t.source().walletId().equals(sourceWalletId)
            && t.idempotencyKey().equals(idempotencyKey))
        .findFirst();
  }

  @Override
  public void add(Transfer transfer) {
    transfers.put(transfer.id(), transfer);
  }

  @Override
  public void update(Transfer transfer) {
    transfers.put(transfer.id(), transfer);
  }

  @Override
  public void publish(TransferEvent event) {
    published.add(event);
  }

  @Override
  public Optional<String> ownerOf(UUID walletId) {
    directoryCalls++;
    if (directoryDown) {
      throw new WalletDirectoryUnavailableException("wallet service is down", null);
    }
    return Optional.ofNullable(owners.get(walletId));
  }
}
