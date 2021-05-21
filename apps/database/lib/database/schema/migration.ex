defmodule Database.Schema.Migration do
  use Database.Schema, storage: :disc_copies

  version do
    attributes([:store, :store_version])
  end
end
