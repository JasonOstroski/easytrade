import { useQuery } from "@tanstack/react-query"
import { Config, FeatureFlag } from "../../../api/featureFlags/types"
import { useQueryContext } from "../QueryContext"
import { configFlagsQuery, problemFlagsQuery } from "./queries"
import { backends } from "../../../api/backend"

export function useProblemFlagsQuery(initialData?: FeatureFlag[]) {
    const { getFeatureFlags } = useQueryContext()
    return useQuery({ ...problemFlagsQuery(getFeatureFlags), initialData })
}

export function useConfigFlagsQuery(initialData?: Config) {
    const { getConfig } = useQueryContext()
    return useQuery({ ...configFlagsQuery(getConfig), initialData })
}

export function useFeatureFlag(flagId: string): boolean {
    const { data } = useQuery({
        queryKey: ["feature-flags", "by-id", flagId],
        queryFn: async () => {
            const res = await backends.config.getById(flagId)
            return res.data
        },
        staleTime: 30_000,
    })
    return data?.enabled ?? false
}
