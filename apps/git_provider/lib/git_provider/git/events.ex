defprotocol GitProvider.Git.Event do
  @spec to_event(GitProvider.Git.Events.t(), GitProvider.Git.Repository.t()) :: Common.Events.t()
  def to_event(event, repository)

  @spec load_content(GitProvider.Git.Events.t()) :: {:ok, term()} | {:error, term()} | :ignore
  def load_content(event)
end

defmodule GitProvider.Git.Events do
  alias GitProvider.Git.Events.Upsert
  alias GitProvider.Git.Specifications

  @type t :: GitProvider.Git.Events.Delete.t() | GitProvider.Git.Events.Upsert.t()

  @spec from_specifications(GitProvider.Git.Specifications.t()) :: [t()]
  def from_specifications(%Specifications{specifications: specifications}) do
    Enum.map(specifications, fn {path, {type, specs}} ->
      case type do
        :openapi ->
          %Upsert.Openapi{path: path, specs: specs}

        :grpc ->
          %Upsert.Grpc{path: path, specs: specs}
      end
    end)
  end
end
