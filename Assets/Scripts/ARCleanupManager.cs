using Unity.XR.CoreUtils;
using UnityEngine;
using UnityEngine.XR.ARFoundation;

public class ARCleanupManager : MonoBehaviour
{
    // Make sure these components are assigned in the Inspector!
    [SerializeField] private ARSession arSession;
    [SerializeField] private XROrigin arSessionOrigin;

    // Static references for easy access from AndroidProjectCommunicate.cs
    public static ARSession SessionReference { get; private set; }
    public static XROrigin OriginReference { get; private set; }

    void Awake()
    {
        // Set the static references
        SessionReference = arSession;
        OriginReference = arSessionOrigin;
    }

    public static void DisableARSession()
    {
        if (SessionReference != null)
        {
            // 1. Disable the ARSession. This is the main cleanup step.
            SessionReference.enabled = false;
        }

        if (OriginReference != null)
        {
            // 2. You might also disable the Session Origin if necessary, 
            // though disabling the Session is usually sufficient to stop tracking.
            OriginReference.enabled = false;
        }
    }
}
