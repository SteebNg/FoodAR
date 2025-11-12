using UnityEngine;
using UnityEngine.Networking;
using System.Collections;
using Firebase.Extensions;
using Firebase.Storage;

public class GetModelFromFirebase : MonoBehaviour
{
    private string foodId;
    [SerializeField] private ARPlaceFood arPlaceFood;
    StorageReference storageRef;

    // Start is called once before the first execution of Update after the MonoBehaviour is created
    void Start()
    {
        storageRef = FirebaseStorage.DefaultInstance.GetReferenceFromUrl("gs://foodar-a85bf.firebasestorage.app/");
    }

    IEnumerator DownloadAllFiles()
    {
        StorageReference model = storageRef.Child("/Foods/" + foodId + "/");

        // 1. Create a flag to track when the Firebase URL task is complete
        bool urlObjTaskCompleted = false;
        bool urlMtlTaskCompleted = false;
        bool urlJpgTaskCompleted = false;
        string objUrl = null;
        string mtlUrl = null;
        string jpgUrl = null;
        System.Exception taskException = null; // Store any exception

        // Start the Firebase task
        model.Child("3DModel.obj").GetDownloadUrlAsync().ContinueWithOnMainThread(task =>
        {
            if (task.IsFaulted || task.IsCanceled)
            {
                taskException = task.Exception;
            }
            else
            {
                objUrl = task.Result.ToString();
            }
            urlObjTaskCompleted = true; // Mark the Firebase task as done
        });
        model.Child("3DModel.mtl").GetDownloadUrlAsync().ContinueWithOnMainThread(task =>
        {
            if (task.IsFaulted || task.IsCanceled)
            {
                taskException = task.Exception;
            }
            else
            {
                mtlUrl = task.Result.ToString();
            }
            urlMtlTaskCompleted = true; // Mark the Firebase task as done
        });
        model.Child("3DModel.jpg").GetDownloadUrlAsync().ContinueWithOnMainThread(task =>
        {
            if (task.IsFaulted || task.IsCanceled)
            {
                taskException = task.Exception;
            }
            else
            {
                jpgUrl = task.Result.ToString();
            }
            urlJpgTaskCompleted = true; // Mark the Firebase task as done
        });

        // 2. WAIT for the Firebase URL task to complete
        yield return new WaitUntil(() => urlObjTaskCompleted);
        yield return new WaitUntil(() => urlMtlTaskCompleted);
        yield return new WaitUntil(() => urlJpgTaskCompleted);

        // 3. Handle errors or proceed with download
        if (taskException != null)
        {
            Debug.LogError("Failed to get OBJ URL: " + taskException.Message);
            yield break; // Exit the coroutine
        }

        // 4. Create a flag for the download coroutine
        bool downloadObjCompleted = false;

        // 5. Start the download coroutine, passing a callback
        StartCoroutine(DownloadFile(objUrl, mtlUrl, jpgUrl, () =>
        {
            downloadObjCompleted = true; // Set flag when DownloadFile finishes
        }));

        // 6. WAIT for the download coroutine to complete
        yield return new WaitUntil(() => downloadObjCompleted);

        Debug.Log("DownloadAllFiles finished successfully!");
    }

    IEnumerator DownloadFile(string objUrl, string mtlUrl, string jpgUrl, System.Action onComplete)
    {
        UnityWebRequest objRequest = UnityWebRequest.Get(objUrl);
        UnityWebRequest mtlRequest = UnityWebRequest.Get(mtlUrl);
        UnityWebRequest jpgRequest = UnityWebRequestTexture.GetTexture(jpgUrl);
        yield return objRequest.SendWebRequest();
        yield return mtlRequest.SendWebRequest();
        yield return jpgRequest.SendWebRequest();

        if (objRequest.result == UnityWebRequest.Result.Success &&
        mtlRequest.result == UnityWebRequest.Result.Success &&
        jpgRequest.result == UnityWebRequest.Result.Success)
        {
            string objText = objRequest.downloadHandler.text;
            string mtlText = mtlRequest.downloadHandler.text;
            Texture2D texture = DownloadHandlerTexture.GetContent(jpgRequest);

            arPlaceFood.IntializeFoodModel(objText, mtlText, texture);

            Debug.Log("GetModel and Textures passed to ARPlaceFood.cs");
        }
        else
        {
            Debug.LogError($"Error downloading model: {objRequest.error}");
        }

        onComplete?.Invoke();
    }
    
    public void SetFoodId(string foodIdGotten)
    {
        foodId = foodIdGotten;

        if (string.IsNullOrEmpty(foodId))
        {
            Debug.LogError("Food ID passed is null or empty.");
            return;
        }

        StartCoroutine(DownloadAllFiles());
    }
}
