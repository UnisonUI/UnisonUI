defmodule Database.Schema.State do
  use Database.Schema, storage: :disc_copies, type: :ordered_set
  require Database

  version do
    attributes([:id, :state])
  end
end
