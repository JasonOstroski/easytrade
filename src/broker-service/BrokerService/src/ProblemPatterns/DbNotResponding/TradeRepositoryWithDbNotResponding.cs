using EasyTrade.BrokerService.Helpers;
using EasyTrade.BrokerService.ProblemPatterns.OpenFeature;

namespace EasyTrade.BrokerService.Entities.Trades.Repository;

public class TradeRepositoryWithDbNotResponding(
    BrokerDbContext dbContext,
    IPluginManager pluginManager,
    IConfiguration configuration,
    ILogger<TradeRepositoryWithDbNotResponding> logger
) : TradeRepository(dbContext)
{
    private readonly IPluginManager _pluginManager = pluginManager;
    private readonly ILogger<TradeRepositoryWithDbNotResponding> _logger = logger;
    private readonly bool _problemPatternsEnabled = string.Equals(
        configuration["PROBLEM_PATTERNS_ENABLED"], "true", StringComparison.OrdinalIgnoreCase
    );

    public override void AddTrade(Trade trade)
    {
        if (_problemPatternsEnabled && CheckIfProblemPatternIsOn())
        {
            _logger.LogWarning(
                "Problem pattern '{Pattern}' is active — injecting invalid trade ID to simulate DB failure",
                Constants.DbNotResponding
            );
            trade.Id = Constants.InvalidTradeId;
        }
        base.AddTrade(trade);
    }

    private bool CheckIfProblemPatternIsOn()
    {
        var task = Task.Run(
            async () => await _pluginManager.GetPluginState(Constants.DbNotResponding, false)
        );
        return task.Result;
    }
}
