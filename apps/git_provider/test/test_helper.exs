Application.stop(:git_provider)
Mox.defmock(Services.Mock, for: Services.Behaviour)
# Mox.stub_with(Services.Mock, ServiceStub)
ExUnit.start()
