Application.stop(:git_provider)
Mox.defmock(Services.Mock, for: Services.Storage)
ExUnit.start()
