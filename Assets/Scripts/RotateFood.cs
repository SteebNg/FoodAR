using UnityEngine;

public class RotateFood : MonoBehaviour
{
    private Transform foodModelPrefab;
    private float initialAngle = 0f;
    private const float rotationSpeed = 70f;
    private int rotationDirection = 0;

    private void Update()
    {
        if (foodModelPrefab != null && rotationDirection != 0)
        {
            float angle = rotationDirection * rotationSpeed * Time.deltaTime;
            foodModelPrefab.Rotate(Vector3.up, angle, Space.World);
        }
    }

    public void StartRotating(int direction)
    {
        rotationDirection = direction;
    }
    
    public void GetPrefab(GameObject foodModel)
    {
        rotationDirection = 0;
        foodModelPrefab = foodModel.transform;
    }
}
