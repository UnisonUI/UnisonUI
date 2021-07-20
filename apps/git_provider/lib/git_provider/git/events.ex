defprotocol GitProvider.Git.Events.ContentLoader do
  @spec load_content(GitProvider.Git.Events.t()) :: {:ok, term()} | {:error, term()}
  def load_content(event)
end

defmodule GitProvider.Git.Events do
  alias GitProvider.Git.Events.Upsert
  alias GitProvider.Git.Specifications
  alias GitProvider.Git.Events.ContentLoader

  @type t :: GitProvider.Git.Events.Delete.t() | GitProvider.Git.Events.Upsert.t()

  @spec from_specifications(GitProvider.Git.Specifications.t(), GitProvider.Git.Repository.t()) ::
          [t()]
  def from_specifications(%Specifications{specifications: specifications}, repository) do
    Enum.map(specifications, fn {path, {type, specs}} ->
      case type do
        :openapi ->
          %Upsert.OpenApi{path: path, specs: specs, repository: repository}

        :grpc ->
          %Upsert.Grpc{path: path, specs: specs, repository: repository}
      end
    end)
  end

  def load_content(event), do: ContentLoader.load_content(event)
end
