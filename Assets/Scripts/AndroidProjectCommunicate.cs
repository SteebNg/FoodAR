using System;
using UnityEngine;

public class AndroidProjectCommunicate : MonoBehaviour
{
    [SerializeField] private GetModelFromFirebase initialScript;
    private string foodId;
    AndroidJavaObject activity;
    private static bool isExiting = false;

    // Start is called once before the first execution of Update after the MonoBehaviour is created
    void Start()
    {
        AndroidJavaClass ajc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
        activity = ajc.GetStatic<AndroidJavaObject>("currentActivity");
        foodId = activity.Call<string>("GetDataForUnity");

        initialScript.SetFoodId(foodId);
    }

    public void BackPressed()
    {
        if (isExiting)
        {
            return;
        }
        isExiting = true;

        ARCleanupManager.DisableARSession();
        Debug.Log("AR Session Cleaned. Exiting Unity");
        activity.Call("ExitUnityActivity");
    }

    void OnApplicationQuit()
    {
        ARCleanupManager.DisableARSession();
        Debug.Log("OnApplicationQuit Called");
    }
}
