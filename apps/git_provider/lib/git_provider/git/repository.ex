defmodule GitProvider.Git.Repository do
  alias GitProvider.Git.Specifications

  @type t :: %__MODULE__{
          service_name: String.t(),
          uri: String.t(),
          name: String.t(),
          branch: String.t(),
          directory: String.t() | nil,
          specifications: [GitProvider.Git.Specifications.t()]
        }
  defstruct [:service_name, :uri, :branch, :directory, :name, specifications: %Specifications{}]

  def create_temp_dir(%__MODULE__{service_name: service_name} = repository) do
    tmp_dir = System.tmp_dir!()

    dir = "unisonui_" <> String.replace(service_name, "/", "_")

    directory = [tmp_dir, dir] |> Path.join() |> Path.expand()

    _ =
      unless File.exists?(directory) do
        _ = File.mkdir(directory)
      end

    %__MODULE__{repository | directory: directory}
  end
end
