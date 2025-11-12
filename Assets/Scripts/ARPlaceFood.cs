using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.XR.ARFoundation;
using UnityEngine.XR.ARSubsystems;
using UnityEditor;
using System.IO;
using System.Text;
using Dummiesman;
using Unity.VisualScripting;
using UnityEngine.EventSystems;

public class ARPlaceFood : MonoBehaviour
{
    private static WaitForSeconds _waitForSeconds0_25 = new WaitForSeconds(0.25f);
    [SerializeField] private ARRaycastManager raycastManager;
    [SerializeField] RotateFood rotateFoodScript;
    [SerializeField] GameObject rotateRightObject;
    [SerializeField] GameObject rotateLeftObject;
    private GameObject foodModelPrefab; 
    GameObject placedObject;
    bool isPlacing = false;
    string objData;
    string mtlData;
    Texture2D textureData;
    private Bounds? _cachedTotalBounds = null;
    private bool isRotating = false;
    private float initialAngle = 0f;
    private const float rotationSpeed = 0.5f;
    private List<ARRaycastHit> rayHits = new List<ARRaycastHit>();
    private PointerEventData eventDataCache;
    private List<RaycastResult> raycastResultsCache = new List<RaycastResult>();
    private Shader modelShader;
    Quaternion currentRotation;

    void Awake()
    {
        if (EventSystem.current != null)
        {
            eventDataCache = new PointerEventData(EventSystem.current);
        }
        modelShader = Shader.Find("Simulation/Standard Lit");
    }

    // Update is called once per frame
    void Update()
    {
        if (!raycastManager || foodModelPrefab == null) // Check if the model is ready
        {
            return;
        }

        if (placedObject != null && Input.touchCount == 2)
        {
            isPlacing = false;
            ManageRotationGesture();
            return;
        }

        if (Input.touchCount == 1)
        {
            Touch touch = Input.GetTouch(0);

            if (IsPointerOverUiObject(touch.position))
            {
                return;
            }

            if (foodModelPrefab != null && touch.phase == TouchPhase.Began && !isPlacing)
            {
                isPlacing = true;
                PlaceObject(Input.GetTouch(0).position);
            }
        }
        else if (foodModelPrefab != null && Input.GetMouseButtonDown(0) && !isPlacing)
        {
            isPlacing = true;
            PlaceObject(Input.mousePosition);
        }
        if (Input.touchCount < 2 && isRotating)
        {
            isRotating = false;
        }
    }

    private void ManageRotationGesture()
    {
        Touch touch0 = Input.GetTouch(0);
        Touch touch1 = Input.GetTouch(1);

        if (touch0.phase == TouchPhase.Moved || touch1.phase == TouchPhase.Moved || isRotating)
        {
            Vector2 touch0InitialPos = touch0.position - touch0.deltaPosition;
            Vector2 touch1InitialPos = touch1.position - touch1.deltaPosition;

            Vector2 currentPosForBothTouch = touch1.position - touch0.position;
            Vector2 previousPosForBothTouch = touch1InitialPos - touch0InitialPos;

            float angleDelta = Vector2.SignedAngle(previousPosForBothTouch, currentPosForBothTouch);

            if (!isRotating)
            {
                // if (IsTouchingOverObject(touch0.position) || IsTouchingOverObject(touch1.position))
                // {
                //     isRotating = true;
                // }
                isRotating = true;
                return;
            }

            if (isRotating)
            {
                placedObject.transform.Rotate(Vector3.up, angleDelta * rotationSpeed, Space.World);
            }
        }

        if (touch0.phase == TouchPhase.Ended || touch0.phase == TouchPhase.Canceled ||
        touch1.phase == TouchPhase.Ended || touch1.phase == TouchPhase.Canceled)
        {
            isRotating = false;
        }
    }
    
    // private bool IsTouchingOverObject(Vector2 screenPosition)
    // {
    //     Ray ray = Camera.main.ScreenPointToRay(screenPosition);
    //     RaycastHit hit;

    //     if (Physics.Raycast(ray, out hit))
    //     {
    //         return hit.transform.IsChildOf(placedObject.transform) || hit.transform == placedObject.transform;
    //     }

    //     return false;
    // }

    void PlaceObject(Vector2 touchPosition)
    {
        rayHits.Clear();
        raycastManager.Raycast(touchPosition, rayHits, TrackableType.AllTypes);

        if (rayHits.Count > 0)
        {
            if (placedObject != null)
            {
                currentRotation = placedObject.transform.rotation;
                Destroy(placedObject);
                placedObject = null;
            }

            // 1. INSTANTIATE the model (create a copy)
            placedObject = Instantiate(foodModelPrefab);
            placedObject.SetActive(true);
            rotateFoodScript.GetPrefab(placedObject);

            // 2. Set the position
            Vector3 hitPosePosition = rayHits[0].pose.position;

            // 3. Use the cached bounds
            float yOffset = _cachedTotalBounds.GetValueOrDefault().extents.y;
            hitPosePosition.y += yOffset;

            placedObject.transform.position = hitPosePosition;

            placedObject.transform.rotation = currentRotation;

            rotateLeftObject.SetActive(true);
            rotateRightObject.SetActive(true);
        }

        StartCoroutine(SetIsPlacingToFalseWithDelay());
    }
    
    private Bounds GetTotalBounds()
    {
        // This method is now only called once inside IntializeFoodModel
        Renderer[] renderers = foodModelPrefab.GetComponentsInChildren<Renderer>();
        if (renderers.Length == 0)
        {
            return new Bounds(foodModelPrefab.transform.position, Vector3.zero);
        }

        Bounds bounds = renderers[0].bounds;
        for (int i = 0; i < renderers.Length; i++)
        {
            bounds.Encapsulate(renderers[i].bounds);
        }

        Vector3 localCenter = foodModelPrefab.transform.InverseTransformPoint(bounds.center);
        Vector3 localExtents = bounds.extents;

        return new Bounds(localCenter, localExtents * 2f);
    }

    IEnumerator SetIsPlacingToFalseWithDelay()
    {
        yield return _waitForSeconds0_25;
        isPlacing = false;
    }
    
    public void IntializeFoodModel(string objText, string mtlText, Texture2D texture) 
    {
        objData = objText;
        mtlData = mtlText;
        textureData = texture;

        if (objData != null && mtlData != null && textureData != null)
        {
            var objStream = new MemoryStream(Encoding.UTF8.GetBytes(objData));
            var mtlStream = new MemoryStream(Encoding.UTF8.GetBytes(mtlData));

            // LOAD THE MODEL ONLY ONCE
            foodModelPrefab = new OBJLoader().Load(objStream, mtlStream);

            // Apply mesh collider to model
            AddColliders(foodModelPrefab);
            
            // Apply materials and texture to the loaded model
            ApplyMaterials(foodModelPrefab, textureData);
            
            // Set it to inactive so it doesn't appear until placed
            foodModelPrefab.SetActive(false);

            // Cache the bounds for efficient placement later
            _cachedTotalBounds = GetTotalBounds();
            
            Debug.Log("ARPlaceFood.cs received and pre-loaded model data successfully.");
        }
        else
        {
            Debug.LogError("Model data is null. Cannot load the model.");
        }
    }

    // Helper method to keep IntializeFoodModel clean
    private void ApplyMaterials(GameObject model, Texture2D texture)
    {
        Renderer[] renderers = model.GetComponentsInChildren<Renderer>();
        foreach (Renderer renderer in renderers)
        {
            Material[] mats = renderer.materials;

            for (int i = 0; i < mats.Length; i++)
            {
                Material mat = mats[i];

                mat.shader = modelShader;

                if (mat.HasProperty("_BaseMap"))
                {
                    mat.SetTexture("_BaseMap", texture);
                }
                else if (mat.HasProperty("_MainTex"))
                {
                    mat.mainTexture = texture;
                }
                else
                {
                    mat.mainTexture = texture;
                }
            }
        }
    }

    private void AddColliders(GameObject model)
    {
        MeshFilter[] meshFilters = model.GetComponentsInChildren<MeshFilter>();
        foreach (MeshFilter meshFilter in meshFilters)
        {
            if (meshFilter.gameObject.GetComponent<MeshCollider>() == null)
            {
                MeshCollider colliderToAdd = meshFilter.gameObject.AddComponent<MeshCollider>();
                colliderToAdd.convex = true; // for complex collider
            }
        }
    }

    private bool IsPointerOverUiObject(Vector2 screenPosition)
    {
        if (EventSystem.current == null || eventDataCache == null)
        {
            return false;
        }

        eventDataCache.position = screenPosition;
        raycastResultsCache.Clear();

        EventSystem.current.RaycastAll(eventDataCache, raycastResultsCache);

        return raycastResultsCache.Count > 0;
    }
}